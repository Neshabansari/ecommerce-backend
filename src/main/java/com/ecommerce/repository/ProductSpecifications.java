package com.ecommerce.repository;

import com.ecommerce.model.Product;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Small building blocks that ProductService combines into one dynamic query. */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    // Matches the text in the name, wood type or description, ignoring case.
    public static Specification<Product> matchesText(String text) {
        String pattern = "%" + escapeLike(text.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.<String>get("name")), pattern, '\\'),
                cb.like(cb.lower(root.<String>get("woodType")), pattern, '\\'),
                cb.like(cb.lower(root.<String>get("description")), pattern, '\\'));
    }

    public static Specification<Product> inCategory(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> priceAtLeast(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), min);
    }

    public static Specification<Product> priceAtMost(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.<BigDecimal>get("price"), max);
    }

    // Stops a user typing % or _ from acting as a wildcard.
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}