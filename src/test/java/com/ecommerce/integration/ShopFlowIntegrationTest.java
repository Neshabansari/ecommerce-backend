package com.ecommerce.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.AbstractIntegrationTest;
import com.ecommerce.model.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

class ShopFlowIntegrationTest extends AbstractIntegrationTest {

    private ResultActions addToCart(String token, Long productId, int quantity) throws Exception {
        return doPost("/api/cart/add", token,
                "{\"productId\":%d,\"quantity\":%d}".formatted(productId, quantity));
    }

    private long placeOrder(String token, Long productId, int quantity) throws Exception {
        addToCart(token, productId, quantity).andExpect(status().isOk());
        return idOf(doPost("/api/orders", token, null).andExpect(status().isCreated()));
    }

    private ResultActions setStatus(String adminToken, long orderId, String newStatus) throws Exception {
        return doPatch("/api/admin/orders/{id}/status", adminToken,
                "{\"status\":\"%s\"}".formatted(newStatus), orderId);
    }

    @Test
    void cartAndOrdersRequireLogin() throws Exception {
        doGet("/api/cart", null).andExpect(status().isUnauthorized());
        doPost("/api/orders", null, null).andExpect(status().isUnauthorized());
    }

    @Test
    void cart_mergesSameProduct_andRespectsStock() throws Exception {
        Product product = createProduct("Cart Chair", "1000.00", 5);
        String token = customerToken();

        addToCart(token, product.getId(), 2).andExpect(status().isOk());
        addToCart(token, product.getId(), 2).andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(4))
                .andExpect(jsonPath("$.totalItems").value(4))
                .andExpect(jsonPath("$.totalAmount").value(4000.0));

        addToCart(token, product.getId(), 3).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("in stock")));
        addToCart(token, 999_999_999L, 1).andExpect(status().isNotFound());

        doDelete("/api/cart/items/{id}", token, product.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void checkout_reducesStock_emptiesCart_andCannotRepeat() throws Exception {
        Product product = createProduct("Checkout Table", "1000.00", 10);
        String token = customerToken();
        addToCart(token, product.getId(), 3).andExpect(status().isOk());

        doPost("/api/orders", token, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(3000.0))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].priceAtPurchase").value(1000.0));

        assertThat(stockOf(product.getId())).isEqualTo(7);
        doGet("/api/cart", token).andExpect(jsonPath("$.items").isEmpty());
        doPost("/api/orders", token, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Your cart is empty"));
    }

    @Test
    void cancelling_restoresStock_andOnlyWorksOnce() throws Exception {
        Product product = createProduct("Cancel Bench", "500.00", 6);
        String token = customerToken();
        long orderId = placeOrder(token, product.getId(), 4);
        assertThat(stockOf(product.getId())).isEqualTo(2);

        doPost("/api/orders/{id}/cancel", token, null, orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(stockOf(product.getId())).isEqualTo(6);

        doPost("/api/orders/{id}/cancel", token, null, orderId).andExpect(status().isConflict());
        assertThat(stockOf(product.getId())).isEqualTo(6);
    }

    @Test
    void ordersArePrivate_andListedNewestFirst() throws Exception {
        Product product = createProduct("Private Shelf", "300.00", 10);
        String owner = customerToken();
        String stranger = customerToken();
        long first = placeOrder(owner, product.getId(), 1);
        long second = placeOrder(owner, product.getId(), 1);

        doGet("/api/orders/{id}", owner, first).andExpect(status().isOk());
        doGet("/api/orders/{id}", stranger, first).andExpect(status().isNotFound());
        doPost("/api/orders/{id}/cancel", stranger, null, first).andExpect(status().isNotFound());
        doGet("/api/orders", owner)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(second));
        doGet("/api/orders", stranger).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void adminMovesAnOrderThroughItsLifecycle() throws Exception {
        Product product = createProduct("Lifecycle Desk", "500.00", 5);
        String customer = customerToken();
        String admin = adminToken();
        long orderId = placeOrder(customer, product.getId(), 2);

        setStatus(customer, orderId, "PAID").andExpect(status().isForbidden());
        setStatus(admin, orderId, "SHIPPED").andExpect(status().isConflict());
        setStatus(admin, orderId, "PAID").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
        doPost("/api/orders/{id}/cancel", customer, null, orderId).andExpect(status().isConflict());
        setStatus(admin, orderId, "SHIPPED").andExpect(status().isOk());
        setStatus(admin, orderId, "CANCELLED").andExpect(status().isConflict());
        setStatus(admin, orderId, "NONSENSE").andExpect(status().isBadRequest());

        doGet("/api/admin/orders/{id}", admin, orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").exists())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
        assertThat(stockOf(product.getId())).isEqualTo(3);
    }

    @Test
    void adminCancel_restoresStock() throws Exception {
        Product product = createProduct("Admin Cancel Stool", "500.00", 5);
        String admin = adminToken();
        long orderId = placeOrder(customerToken(), product.getId(), 3);
        assertThat(stockOf(product.getId())).isEqualTo(2);

        setStatus(admin, orderId, "CANCELLED").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(stockOf(product.getId())).isEqualTo(5);
    }

    @Test
    void adminEndpoints_areAdminOnly() throws Exception {
        String customer = customerToken();
        String admin = adminToken();

        for (String url : List.of("/api/admin/orders", "/api/admin/users", "/api/admin/reports/summary")) {
            doGet(url, null).andExpect(status().isUnauthorized());
            doGet(url, customer).andExpect(status().isForbidden());
            doGet(url, admin).andExpect(status().isOk());
        }
        doGet("/api/admin/orders?status=BOGUS", admin).andExpect(status().isBadRequest());
        doGet("/api/admin/reports/summary?lowStockThreshold=-1", admin).andExpect(status().isBadRequest());
        doGet("/api/admin/users", admin).andExpect(jsonPath("$.content[0].password").doesNotExist());
    }

    @Test
    void salesReport_countsOnlyPaidOrders() throws Exception {
        Product product = createProduct("Report Cabinet", "700.00", 5);
        String admin = adminToken();
        long orderId = placeOrder(customerToken(), product.getId(), 2);
        setStatus(admin, orderId, "PAID").andExpect(status().isOk());
        setStatus(admin, orderId, "SHIPPED").andExpect(status().isOk());

        doGet("/api/admin/reports/summary?lowStockThreshold=100", admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenue").isNumber())
                .andExpect(jsonPath("$.ordersByStatus.SHIPPED", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.topProducts").isArray())
                .andExpect(jsonPath("$.lowStockProducts").isArray());
    }

    @Test
    void lastItemCanOnlyBeBoughtOnce() throws Exception {
        Product lastOne = createProduct("Last One", "999.00", 1);
        String buyerA = customerToken();
        String buyerB = customerToken();
        // Both carts are valid, because the stock (1) covers one unit each.
        addToCart(buyerA, lastOne.getId(), 1).andExpect(status().isOk());
        addToCart(buyerB, lastOne.getId(), 1).andExpect(status().isOk());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (String token : List.of(buyerA, buyerB)) {
            results.add(pool.submit(() -> {
                start.await();
                return doPost("/api/orders", token, null).andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> result : results) {
            statuses.add(result.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(statuses).containsExactlyInAnyOrder(201, 400);
        assertThat(stockOf(lastOne.getId())).isZero();
    }
}