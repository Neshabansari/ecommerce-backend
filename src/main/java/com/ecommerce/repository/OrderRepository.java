package com.ecommerce.repository;

import com.ecommerce.model.Order;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // A user's order history, newest first.
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
}