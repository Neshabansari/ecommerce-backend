package com.ecommerce.service;

import com.ecommerce.dto.LowStockProductResponse;
import com.ecommerce.dto.SalesReportResponse;
import com.ecommerce.dto.TopProductResponse;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    // Revenue and best-sellers count only orders that were paid for.
    private static final List<OrderStatus> REVENUE_STATUSES = List.of(OrderStatus.PAID, OrderStatus.SHIPPED);
    private static final int TOP_PRODUCTS_LIMIT = 5;

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public SalesReportResponse summary(int lowStockThreshold) {
        if (lowStockThreshold < 0) {
            throw new BadRequestException("lowStockThreshold cannot be negative");
        }

        Map<OrderStatus, Long> ordersByStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            ordersByStatus.put(status, orderRepository.countByStatus(status));
        }

        BigDecimal revenue = Objects.requireNonNullElse(
                orderRepository.sumTotalByStatusIn(REVENUE_STATUSES), BigDecimal.ZERO);

        List<TopProductResponse> topProducts = orderRepository.findTopSelling(
                REVENUE_STATUSES, PageRequest.of(0, TOP_PRODUCTS_LIMIT));

        List<LowStockProductResponse> lowStock = productRepository
                .findByStockQuantityLessThanEqualOrderByStockQuantityAscNameAsc(lowStockThreshold).stream()
                .map(product -> new LowStockProductResponse(
                        product.getId(), product.getName(), product.getStockQuantity()))
                .toList();

        return new SalesReportResponse(userRepository.count(), productRepository.count(),
                ordersByStatus, revenue, topProducts, lowStockThreshold, lowStock);
    }
}