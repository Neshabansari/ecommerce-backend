package com.ecommerce.service;

import com.ecommerce.dto.OrderResponse;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ConflictException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.Cart;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.Product;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    /** Turns the user's cart into an order. Everything happens in one transaction. */
    @Transactional
    public OrderResponse checkout(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        Map<Long, Product> products = lockProducts(
                cart.getItems().stream().map(item -> item.getProduct().getId()).toList());

        Order order = Order.builder()
                .user(cart.getUser())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = products.get(cartItem.getProduct().getId());
            int quantity = cartItem.getQuantity();
            if (quantity > product.getStockQuantity()) {
                throw new BadRequestException("Not enough stock for '" + product.getName()
                        + "'. Available: " + product.getStockQuantity() + ", in your cart: " + quantity);
            }
            product.setStockQuantity(product.getStockQuantity() - quantity);
            // The price is copied now, so later price changes never alter this order.
            order.addItem(OrderItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .priceAtPurchase(product.getPrice())
                    .build());
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);
        new ArrayList<>(cart.getItems()).forEach(cart::removeItem);
        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(Long userId, Long orderId) {
        return OrderResponse.from(findOwned(userId, orderId));
    }

    /** A user can cancel their own PENDING order. The stock goes back on the shelf. */
    @Transactional
    public OrderResponse cancel(Long userId, Long orderId) {
        Order order = findOwned(userId, orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException(
                    "Only PENDING orders can be cancelled. This order is " + order.getStatus());
        }
        restockAndCancel(order);
        return OrderResponse.from(order);
    }

    /** Puts the stock back and marks the order CANCELLED. Shared by user and admin cancellation. */
    @Transactional
    public void restockAndCancel(Order order) {
        Map<Long, Product> products = lockProducts(
                order.getItems().stream().map(item -> item.getProduct().getId()).toList());
        for (OrderItem item : order.getItems()) {
            Product product = products.get(item.getProduct().getId());
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED);
    }

    // A missing order and someone else's order both give 404, so ids cannot be probed.
    private Order findOwned(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    private Map<Long, Product> lockProducts(Collection<Long> ids) {
        return productRepository.findAllByIdForUpdate(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }
}