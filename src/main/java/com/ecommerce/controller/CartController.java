package com.ecommerce.controller;

import com.ecommerce.dto.AddToCartRequest;
import com.ecommerce.dto.CartResponse;
import com.ecommerce.dto.UpdateCartItemRequest;
import com.ecommerce.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponse get(@AuthenticationPrincipal Jwt jwt) {
        return cartService.getCart(userId(jwt));
    }

    @PostMapping("/add")
    public CartResponse add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddToCartRequest request) {
        return cartService.addItem(userId(jwt), request);
    }

    @PutMapping("/items/{productId}")
    public CartResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId,
                               @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(userId(jwt), productId, request);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
        return cartService.removeItem(userId(jwt), productId);
    }

    @DeleteMapping
    public CartResponse clear(@AuthenticationPrincipal Jwt jwt) {
        return cartService.clear(userId(jwt));
    }

    // The user id always comes from the token, never from the request.
    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}