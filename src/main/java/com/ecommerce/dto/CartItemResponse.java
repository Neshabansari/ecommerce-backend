package com.ecommerce.dto;

import com.ecommerce.model.CartItem;
import com.ecommerce.model.Product;
import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal,
        Integer availableStock) {

    public static CartItemResponse from(CartItem item) {
        Product product = item.getProduct();
        return new CartItemResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                item.getQuantity(),
                product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                product.getStockQuantity());
    }
}