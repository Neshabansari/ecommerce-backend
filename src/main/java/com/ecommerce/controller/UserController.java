package com.ecommerce.controller;

import com.ecommerce.dto.UserResponse;
import com.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // The user id comes from the token's "sub" claim, never from the request.
    @GetMapping("/profile")
    public UserResponse profile(@AuthenticationPrincipal Jwt jwt) {
        return userService.getProfile(Long.valueOf(jwt.getSubject()));
    }
}