package com.ecommerce.dto;

import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record AdminOrderResponse(
        Long id,
        Long userId,
        String userEmail,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        List<OrderItemResponse> items) {

    public static AdminOrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .sorted(Comparator.comparing((OrderItem item) -> item.getProduct().getName()))
                .map(OrderItemResponse::from)
                .toList();
        return new AdminOrderResponse(order.getId(), order.getUser().getId(), order.getUser().getEmail(),
                order.getStatus(), order.getTotalAmount(), order.getCreatedAt(), items);
    }
}