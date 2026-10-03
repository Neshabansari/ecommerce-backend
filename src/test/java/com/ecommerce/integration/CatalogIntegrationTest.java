package com.ecommerce.integration;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.AbstractIntegrationTest;
import com.ecommerce.model.Category;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogIntegrationTest extends AbstractIntegrationTest {

    private String productBody(String name, String price, int stock, long categoryId) {
        return ("{\"name\":\"%s\",\"description\":\"Test description\",\"price\":%s,"
                + "\"stockQuantity\":%d,\"woodType\":\"Oak\",\"categoryId\":%d}")
                .formatted(name, price, stock, categoryId);
    }

    private String categoryBody(String name) {
        return "{\"name\":\"%s\",\"description\":\"Test category\"}".formatted(name);
    }

    @Test
    void anyoneCanBrowseProductsAndCategories() throws Exception {
        doGet("/api/products", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0));
        doGet("/api/categories", null).andExpect(status().isOk());
    }

    @Test
    void anonymousAndCustomerCannotCreateProducts() throws Exception {
        Category category = createCategory();
        String body = productBody("Nope " + UUID.randomUUID(), "100.00", 1, category.getId());

        doPost("/api/products", null, body).andExpect(status().isUnauthorized());
        doPost("/api/products", customerToken(), body).andExpect(status().isForbidden());
    }

    @Test
    void adminManagesTheProductLifecycle() throws Exception {
        String admin = adminToken();
        long categoryId = idOf(doPost("/api/categories", admin, categoryBody("Cat " + UUID.randomUUID()))
                .andExpect(status().isCreated()));
        String name = "Oak Stool " + UUID.randomUUID();

        long productId = idOf(doPost("/api/products", admin, productBody(name, "8500.00", 10, categoryId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name)));

        doGet("/api/products/{id}", null, productId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(categoryId));

        doPut("/api/products/{id}", admin, productBody(name, "9200.00", 10, categoryId), productId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(9200.0));

        doDelete("/api/products/{id}", admin, productId).andExpect(status().isNoContent());
        doGet("/api/products/{id}", null, productId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id " + productId));
    }

    @Test
    void invalidProduct_returnsFieldErrors() throws Exception {
        String body = "{\"name\":\"ab\",\"description\":\"\",\"price\":-5,\"stockQuantity\":-1}";

        doPost("/api/products", adminToken(), body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.price").exists())
                .andExpect(jsonPath("$.validationErrors.stockQuantity").exists())
                .andExpect(jsonPath("$.validationErrors.categoryId").exists());
    }

    @Test
    void productWithUnknownCategory_isNotFound() throws Exception {
        doPost("/api/products", adminToken(), productBody("Ghost Table", "100.00", 1, 999_999_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchFiltersSortsAndPages() throws Exception {
        Category category = createCategory();
        String key = "zq" + UUID.randomUUID().toString().substring(0, 8);
        createProduct(key + " cheap", "100.00", 5, category);
        createProduct(key + " mid", "200.00", 5, category);
        createProduct(key + " pricey", "300.00", 5, category);

        doGet("/api/products?search=" + key, null)
                .andExpect(jsonPath("$.totalElements").value(3));
        doGet("/api/products?search=" + key.toUpperCase(), null)
                .andExpect(jsonPath("$.totalElements").value(3));
        doGet("/api/products?search=" + key + "&minPrice=150", null)
                .andExpect(jsonPath("$.totalElements").value(2));
        doGet("/api/products?search=" + key + "&maxPrice=250&sortBy=price&direction=desc", null)
                .andExpect(jsonPath("$.content[0].name").value(key + " mid"));
        doGet("/api/products?categoryId=" + category.getId() + "&sortBy=price&direction=asc", null)
                .andExpect(jsonPath("$.content[0].name").value(key + " cheap"));
        doGet("/api/products?search=" + key + "&size=2&page=1", null)
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.last").value(true));
        doGet("/api/products/search?search=" + key, null)
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void badSearchParameters_areRejected_andPageSizeIsCapped() throws Exception {
        doGet("/api/products?sortBy=password", null).andExpect(status().isBadRequest());
        doGet("/api/products?direction=sideways", null).andExpect(status().isBadRequest());
        doGet("/api/products?minPrice=500&maxPrice=100", null).andExpect(status().isBadRequest());
        doGet("/api/products/abc", null).andExpect(status().isBadRequest());
        doGet("/api/products?size=1000", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void categoryRules_duplicatesAndDeletion() throws Exception {
        String admin = adminToken();
        String name = "Cat " + UUID.randomUUID();
        long id = idOf(doPost("/api/categories", admin, categoryBody(name)).andExpect(status().isCreated()));

        doPost("/api/categories", admin, categoryBody(name.toUpperCase()))
                .andExpect(status().isConflict());

        Category withProducts = createCategory();
        createProduct("Blocker " + UUID.randomUUID(), "100.00", 1, withProducts);
        doDelete("/api/categories/{id}", admin, withProducts.getId()).andExpect(status().isConflict());

        doDelete("/api/categories/{id}", admin, id).andExpect(status().isNoContent());
        doGet("/api/categories/{id}", null, id).andExpect(status().isNotFound());
        doGet("/api/categories", null)
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
    }
}