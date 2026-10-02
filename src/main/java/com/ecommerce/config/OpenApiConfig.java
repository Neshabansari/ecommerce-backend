package com.ecommerce.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ashgrove E-commerce Backend API")
                        .version("1.0.0")
                        .description("REST API for a furniture shop: catalog, accounts, cart, orders and "
                                + "administration. Prices are in NPR. Log in with POST /api/auth/login, "
                                + "then click Authorize and paste the accessToken."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .tags(List.of(
                        new Tag().name("Authentication").description("Register and log in"),
                        new Tag().name("Users").description("The signed-in user's profile"),
                        new Tag().name("Categories").description("Browse categories; admins manage them"),
                        new Tag().name("Products").description("Browse, search and filter; admins manage products"),
                        new Tag().name("Cart").description("The signed-in user's shopping cart"),
                        new Tag().name("Orders").description("Checkout, order history and cancellation"),
                        new Tag().name("Admin").description("Order management, users and reports (ADMIN only)")));
    }

    // Groups every endpoint by feature and adds the token requirement to the protected ones,
    // so the controllers need no documentation annotations.
    @Bean
    public OpenApiCustomizer endpointCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, item) ->
                item.readOperationsMap().forEach((method, operation) -> {
                    String tag = tagFor(path);
                    if (tag != null) {
                        operation.setTags(List.of(tag));
                    }
                    if (!isPublic(path, method)) {
                        operation.addSecurityItem(new SecurityRequirement().addList(BEARER));
                    }
                }));
    }

    private static boolean isPublic(String path, PathItem.HttpMethod method) {
        return path.startsWith("/api/auth/")
                || (method == PathItem.HttpMethod.GET
                && (path.startsWith("/api/products") || path.startsWith("/api/categories")));
    }

    private static String tagFor(String path) {
        if (path.startsWith("/api/auth")) return "Authentication";
        if (path.startsWith("/api/users")) return "Users";
        if (path.startsWith("/api/categories")) return "Categories";
        if (path.startsWith("/api/products")) return "Products";
        if (path.startsWith("/api/cart")) return "Cart";
        if (path.startsWith("/api/orders")) return "Orders";
        if (path.startsWith("/api/admin")) return "Admin";
        return null;
    }
}