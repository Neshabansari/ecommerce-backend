package com.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ConflictException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @InjectMocks private ProductService productService;

    @Test
    void search_minPriceAboveMaxPrice_isRejected() {
        assertThatThrownBy(() -> productService.search(null, null, new BigDecimal("500"),
                new BigDecimal("100"), 0, 10, "name", "asc"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void search_unknownSortField_isRejected() {
        assertThatThrownBy(() -> productService.search(null, null, null, null, 0, 10, "password", "asc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("sortBy");
    }

    @Test
    void search_badDirection_isRejected() {
        assertThatThrownBy(() -> productService.search(null, null, null, null, 0, 10, "name", "sideways"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("direction");
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_capsPageSize_andAddsIdAsSecondSortKey() {
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        productService.search("oak", null, null, null, 0, 1000, "name", "asc");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        assertThat(pageable.getValue().getSort().stream().map(Sort.Order::getProperty))
                .containsExactly("name", "id");
    }

    @Test
    void create_trimsTextAndLinksCategory() {
        Category category = Category.builder().id(3L).name("Seating").build();
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.create(new ProductRequest(
                "  Oak Stool ", " A stool. ", new BigDecimal("8500.00"), 10, " ", 3L));

        assertThat(response.name()).isEqualTo("Oak Stool");
        assertThat(response.description()).isEqualTo("A stool.");
        assertThat(response.woodType()).isNull();
        assertThat(response.categoryId()).isEqualTo(3L);
    }

    @Test
    void create_unknownCategory_throwsNotFound() {
        when(categoryRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(new ProductRequest(
                "Oak Stool", "A stool.", new BigDecimal("8500.00"), 10, null, 9L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_productAlreadyInAnOrder_becomesConflict() {
        Product product = Product.builder().id(1L).name("Table").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doThrow(new DataIntegrityViolationException("foreign key")).when(productRepository).flush();

        assertThatThrownBy(() -> productService.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("stock to 0");
    }
}