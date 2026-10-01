package com.ecommerce.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Security failures happen before a request reaches a controller, so the
 * GlobalExceptionHandler cannot catch them. This class writes the same JSON
 * error shape for 401 (not logged in) and 403 (not allowed).
 */
@Component
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        write(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                "Authentication is required. Please log in.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(request, response, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                "You do not have permission to do this.");
    }

    private void write(HttpServletRequest request, HttpServletResponse response,
                       int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String path = request.getRequestURI().replace("\\", "\\\\").replace("\"", "\\\"");
        String body = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\","
                        + "\"path\":\"%s\",\"validationErrors\":{}}",
                Instant.now(), status, error, message, path);
        response.getWriter().write(body);
    }
}