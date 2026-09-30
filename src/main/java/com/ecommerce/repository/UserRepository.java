package com.ecommerce.repository;

import com.ecommerce.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    // Used by login: find a user by their email.
    Optional<User> findByEmail(String email);

    // Used by registration: reject an email that is already taken.
    boolean existsByEmail(String email);
}