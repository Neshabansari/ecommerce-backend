package com.ecommerce.dto;

import com.ecommerce.model.Role;

// Deliberately has no password field.
public record UserResponse(Long id, String fullName, String email, Role role) {
}