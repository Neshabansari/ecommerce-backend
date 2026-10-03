package com.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ecommerce.dto.OrderResponse;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ConflictException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.Cart;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private ProductRepository productRepository;
    @InjectMocks private OrderService orderService;

    private User user;
    private Cart cart;
    private Product chair;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("u@example.com").build();
        cart = Cart.builder().id(10L).user(user).build();
        chair = Product.builder().id(7L).name("Windsor Chair")
                .price(new BigDecimal("18500.00")).stockQuantity(12).build();
    }

    @Test
    void checkout_reducesStock_totalsOrder_andEmptiesCart() {
        cart.addItem(CartItem.builder().product(chair).quantity(4).build());
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findAllByIdForUpdate(any())).thenReturn(List.of(chair));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.checkout(1L);

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo("74000.00");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(4);
        assertThat(chair.getStockQuantity()).isEqualTo(8);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void checkout_copiesPrice_soLaterPriceChangesDoNotAlterTheOrder() {
        cart.addItem(CartItem.builder().product(chair).quantity(1).build());
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findAllByIdForUpdate(any())).thenReturn(List.of(chair));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.checkout(1L);
        chair.setPrice(new BigDecimal("99999.00"));

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertThat(saved.getValue().getItems().get(0).getPriceAtPurchase()).isEqualByComparingTo("18500.00");
    }

    @Test
    void checkout_emptyCart_isRejected() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.checkout(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Your cart is empty");

        verifyNoInteractions(productRepository);
    }

    @Test
    void checkout_notEnoughStockAnymore_isRejectedAndNothingChanges() {
        cart.addItem(CartItem.builder().product(chair).quantity(4).build());
        chair.setStockQuantity(2);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findAllByIdForUpdate(any())).thenReturn(List.of(chair));

        assertThatThrownBy(() -> orderService.checkout(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Not enough stock");

        assertThat(chair.getStockQuantity()).isEqualTo(2);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancel_pendingOrder_restoresStock() {
        chair.setStockQuantity(8);
        Order order = Order.builder().id(5L).user(user).status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("74000.00")).build();
        order.addItem(OrderItem.builder().product(chair).quantity(4)
                .priceAtPurchase(new BigDecimal("18500.00")).build());
        when(orderRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(order));
        when(productRepository.findAllByIdForUpdate(any())).thenReturn(List.of(chair));

        OrderResponse response = orderService.cancel(1L, 5L);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(chair.getStockQuantity()).isEqualTo(12);
    }

    @Test
    void cancel_shippedOrder_isRejected() {
        Order order = Order.builder().id(5L).user(user).status(OrderStatus.SHIPPED)
                .totalAmount(BigDecimal.TEN).build();
        when(orderRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(1L, 5L)).isInstanceOf(ConflictException.class);

        verifyNoInteractions(productRepository);
    }

    @Test
    void cancel_someoneElsesOrder_looksLikeNotFound() {
        when(orderRepository.findByIdAndUserId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancel(2L, 5L)).isInstanceOf(ResourceNotFoundException.class);
    }
}