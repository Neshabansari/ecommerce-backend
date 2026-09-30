package com.ecommerce.repository;

import com.ecommerce.model.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

    // Every user has one cart.
    Optional<Cart> findByUserId(Long userId);
}