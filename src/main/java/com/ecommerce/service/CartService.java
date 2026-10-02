package com.ecommerce.service;

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
import java.util.ArrayList;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public CartResponse getCart(Long userId) {
        return CartResponse.from(cartFor(userId));
    }

    @Transactional
    public CartResponse addItem(Long userId, AddToCartRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId()));
        Cart cart = cartFor(userId);

        CartItem existing = findItem(cart, product.getId()).orElse(null);
        int newQuantity = request.quantity() + (existing == null ? 0 : existing.getQuantity());
        checkStock(product, newQuantity);

        if (existing == null) {
            cart.addItem(CartItem.builder().product(product).quantity(newQuantity).build());
        } else {
            existing.setQuantity(newQuantity);
        }
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse updateItem(Long userId, Long productId, UpdateCartItemRequest request) {
        Cart cart = cartFor(userId);
        CartItem item = findItem(cart, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " is not in your cart"));
        checkStock(item.getProduct(), request.quantity());
        item.setQuantity(request.quantity());
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = cartFor(userId);
        CartItem item = findItem(cart, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " is not in your cart"));
        cart.removeItem(item);
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse clear(Long userId) {
        Cart cart = cartFor(userId);
        new ArrayList<>(cart.getItems()).forEach(cart::removeItem);
        return CartResponse.from(cart);
    }

    // Every user gets a cart at registration; this also covers accounts created any other way.
    private Cart cartFor(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            return cartRepository.save(Cart.builder().user(user).build());
        });
    }

    private Optional<CartItem> findItem(Cart cart, Long productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();
    }

    private void checkStock(Product product, int quantity) {
        if (quantity > product.getStockQuantity()) {
            throw new BadRequestException("Only " + product.getStockQuantity()
                    + " unit(s) of '" + product.getName() + "' in stock");
        }
    }
}