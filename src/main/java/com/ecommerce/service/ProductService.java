package com.ecommerce.service;

import com.ecommerce.dto.PageResponse;
import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ConflictException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductSpecifications;
import java.math.BigDecimal;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Set<String> SORT_FIELDS = Set.of("name", "price", "createdAt", "stockQuantity");
    private static final int MAX_PAGE_SIZE = 50;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String search, Long categoryId, BigDecimal minPrice,
                                                BigDecimal maxPrice, int page, int size,
                                                String sortBy, String direction) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice cannot be greater than maxPrice");
        }
        if (!SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("sortBy must be one of: name, price, createdAt, stockQuantity");
        }
        if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
            throw new BadRequestException("direction must be 'asc' or 'desc'");
        }

        Specification<Product> spec = (root, query, cb) -> cb.conjunction();
        if (search != null && !search.isBlank()) {
            spec = spec.and(ProductSpecifications.matchesText(search.trim()));
        }
        if (categoryId != null) {
            spec = spec.and(ProductSpecifications.inCategory(categoryId));
        }
        if (minPrice != null) {
            spec = spec.and(ProductSpecifications.priceAtLeast(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(ProductSpecifications.priceAtMost(maxPrice));
        }

        // The id is a second sort key so pages stay stable when many products tie on the first one.
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy).and(Sort.by("id"));
        Pageable pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);

        return PageResponse.from(productRepository.findAll(spec, pageable).map(ProductResponse::from));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return ProductResponse.from(find(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = Product.builder().build();
        apply(product, request);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = find(id);
        apply(product, request);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        Product product = find(id);
        try {
            productRepository.delete(product);
            productRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(
                    "This product is in a cart or an order and cannot be deleted. Set its stock to 0 instead.");
        }
    }

    private void apply(Product product, ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
        product.setName(request.name().trim());
        product.setDescription(request.description().trim());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setWoodType(request.woodType() == null || request.woodType().isBlank()
                ? null : request.woodType().trim());
        product.setCategory(category);
    }

    private Product find(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }
}