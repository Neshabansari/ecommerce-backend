package com.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ecommerce.dto.AdminOrderResponse;
import com.ecommerce.dto.PageResponse;
import com.ecommerce.exception.ConflictException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.User;
import com.ecommerce.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderService orderService;
    @InjectMocks private AdminOrderService adminOrderService;

    private Order orderWithStatus(OrderStatus status) {
        User user = User.builder().id(1L).email("u@example.com").build();
        return Order.builder().id(5L).user(user).status(status).totalAmount(BigDecimal.TEN).build();
    }

    @ParameterizedTest
    @CsvSource({"PENDING,PAID", "PAID,SHIPPED"})
    void updateStatus_forwardMoves_areAllowed(OrderStatus from, OrderStatus to) {
        Order order = orderWithStatus(from);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        AdminOrderResponse response = adminOrderService.updateStatus(5L, to);

        assertThat(response.status()).isEqualTo(to);
        verifyNoInteractions(orderService);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PENDING", "PAID"})
    void updateStatus_cancel_goesThroughRestocking(OrderStatus from) {
        Order order = orderWithStatus(from);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        adminOrderService.updateStatus(5L, OrderStatus.CANCELLED);

        verify(orderService).restockAndCancel(order);
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING,SHIPPED", "PENDING,PENDING", "PAID,PENDING", "PAID,PAID",
            "SHIPPED,CANCELLED", "SHIPPED,PAID", "CANCELLED,PAID", "CANCELLED,PENDING"})
    void updateStatus_invalidMoves_areRejected(OrderStatus from, OrderStatus to) {
        when(orderRepository.findById(5L)).thenReturn(Optional.of(orderWithStatus(from)));

        assertThatThrownBy(() -> adminOrderService.updateStatus(5L, to))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(from.name());

        verify(orderService, never()).restockAndCancel(any(Order.class));
    }

    @Test
    void updateStatus_unknownOrder_throwsNotFound() {
        when(orderRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminOrderService.updateStatus(5L, OrderStatus.PAID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void list_capsPageSizeAndFixesNegativePage() {
        when(orderRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(orderWithStatus(OrderStatus.PENDING))));

        PageResponse<AdminOrderResponse> response = adminOrderService.list(null, -3, 1000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findAll(pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        assertThat(response.content()).hasSize(1);
    }
}