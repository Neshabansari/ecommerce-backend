package com.ecommerce.controller;

import com.ecommerce.dto.AdminOrderResponse;
import com.ecommerce.dto.PageResponse;
import com.ecommerce.dto.UpdateOrderStatusRequest;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.service.AdminOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Everything under /api/admin/** is limited to ADMIN in SecurityConfig.
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public PageResponse<AdminOrderResponse> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return adminOrderService.list(status, page, size);
    }

    @GetMapping("/{id}")
    public AdminOrderResponse get(@PathVariable Long id) {
        return adminOrderService.get(id);
    }

    @PatchMapping("/{id}/status")
    public AdminOrderResponse updateStatus(@PathVariable Long id,
                                           @Valid @RequestBody UpdateOrderStatusRequest request) {
        return adminOrderService.updateStatus(id, request.status());
    }
}