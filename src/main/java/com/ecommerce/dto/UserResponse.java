package com.ecommerce.dto;

import com.ecommerce.model.Role;
import com.ecommerce.model.User;

// Deliberately has no password field.
public record UserResponse(Long id, String fullName, String email, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}