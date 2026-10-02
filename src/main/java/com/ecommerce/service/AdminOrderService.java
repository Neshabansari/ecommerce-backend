package com.ecommerce.service;

import com.ecommerce.dto.AdminOrderResponse;
import com.ecommerce.dto.PageResponse;
import com.ecommerce.exception.ConflictException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.repository.OrderRepository;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private static final int MAX_PAGE_SIZE = 50;

    // The only allowed moves. SHIPPED and CANCELLED are final.
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),
            OrderStatus.PAID, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, Set.of(),
            OrderStatus.CANCELLED, Set.of());

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Transactional(readOnly = true)
    public PageResponse<AdminOrderResponse> list(OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<Order> result = status == null
                ? orderRepository.findAll(pageable)
                : orderRepository.findByStatus(status, pageable);
        return PageResponse.from(result.map(AdminOrderResponse::from));
    }

    @Transactional(readOnly = true)
    public AdminOrderResponse get(Long id) {
        return AdminOrderResponse.from(find(id));
    }

    @Transactional
    public AdminOrderResponse updateStatus(Long id, OrderStatus target) {
        Order order = find(id);
        if (!ALLOWED.get(order.getStatus()).contains(target)) {
            throw new ConflictException(
                    "Cannot change order from " + order.getStatus() + " to " + target);
        }
        if (target == OrderStatus.CANCELLED) {
            orderService.restockAndCancel(order);
        } else {
            order.setStatus(target);
        }
        return AdminOrderResponse.from(order);
    }

    private Order find(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }
}