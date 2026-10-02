package com.ecommerce.dto;

import java.math.BigDecimal;

public record TopProductResponse(Long productId, String productName, Long unitsSold, BigDecimal revenue) {
}