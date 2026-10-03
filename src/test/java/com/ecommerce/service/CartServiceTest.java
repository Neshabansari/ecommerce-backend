package com.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.dto.AddToCartRequest;
import com.ecommerce.dto.CartResponse;
import com.ecommerce.dto.UpdateCartItemRequest;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.Cart;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @InjectMocks private CartService cartService;

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
    void addItem_newProduct_createsLineWithTotals() {
        when(productRepository.findById(7L)).thenReturn(Optional.of(chair));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.addItem(1L, new AddToCartRequest(7L, 2));

        assertThat(response.items()).hasSize(1);
        assertThat(response.totalItems()).isEqualTo(2);
        assertThat(response.totalAmount()).isEqualByComparingTo("37000.00");
    }

    @Test
    void addItem_sameProductTwice_mergesIntoOneLine() {
        when(productRepository.findById(7L)).thenReturn(Optional.of(chair));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        cartService.addItem(1L, new AddToCartRequest(7L, 2));
        CartResponse response = cartService.addItem(1L, new AddToCartRequest(7L, 3));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(5);
    }

    @Test
    void addItem_moreThanStock_isRejected() {
        when(productRepository.findById(7L)).thenReturn(Optional.of(chair));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.addItem(1L, new AddToCartRequest(7L, 13)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only 12");

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void addItem_mergedQuantityOverStock_isRejected() {
        when(productRepository.findById(7L)).thenReturn(Optional.of(chair));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        cartService.addItem(1L, new AddToCartRequest(7L, 10));

        assertThatThrownBy(() -> cartService.addItem(1L, new AddToCartRequest(7L, 3)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void addItem_unknownProduct_throwsNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(1L, new AddToCartRequest(99L, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateItem_changesQuantity() {
        cart.addItem(CartItem.builder().product(chair).quantity(1).build());
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.updateItem(1L, 7L, new UpdateCartItemRequest(4));

        assertThat(response.items().get(0).quantity()).isEqualTo(4);
    }

    @Test
    void updateItem_overStock_isRejected() {
        cart.addItem(CartItem.builder().product(chair).quantity(1).build());
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateItem(1L, 7L, new UpdateCartItemRequest(13)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateItem_productNotInCart_throwsNotFound() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateItem(1L, 7L, new UpdateCartItemRequest(2)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not in your cart");
    }

    @Test
    void removeItem_removesTheLine() {
        cart.addItem(CartItem.builder().product(chair).quantity(2).build());
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.removeItem(1L, 7L);

        assertThat(response.items()).isEmpty();
        assertThat(response.totalAmount()).isEqualByComparingTo("0");
    }

    @Test
    void clear_emptiesTheCart() {
        cart.addItem(CartItem.builder().product(chair).quantity(2).build());
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThat(cartService.clear(1L).items()).isEmpty();
    }

    @Test
    void getCart_createsCartWhenUserHasNone() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(cartService.getCart(1L).items()).isEmpty();
        verify(cartRepository).save(any(Cart.class));
    }
}