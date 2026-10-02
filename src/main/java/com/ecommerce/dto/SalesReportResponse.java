package com.ecommerce.dto;

import com.ecommerce.model.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record SalesReportResponse(
        long totalUsers,
        long totalProducts,
        Map<OrderStatus, Long> ordersByStatus,
        BigDecimal revenue,
        List<TopProductResponse> topProducts,
        int lowStockThreshold,
        List<LowStockProductResponse> lowStockProducts) {
}