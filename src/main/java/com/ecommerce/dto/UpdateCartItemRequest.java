package com.ecommerce.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1 (use DELETE to remove an item)")
        @Max(value = 100, message = "Quantity cannot be more than 100")
        Integer quantity) {
}