package com.ecommerce.dto;

public record LowStockProductResponse(Long id, String name, Integer stockQuantity) {
}