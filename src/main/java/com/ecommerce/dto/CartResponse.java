package com.ecommerce.dto;

import com.ecommerce.model.Cart;
import com.ecommerce.model.CartItem;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record CartResponse(
        Long id,
        List<CartItemResponse> items,
        int totalItems,
        BigDecimal totalAmount) {

    public static CartResponse from(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .sorted(Comparator.comparing((CartItem item) -> item.getProduct().getName()))
                .map(CartItemResponse::from)
                .toList();
        int totalItems = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal total = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), items, totalItems, total);
    }
}