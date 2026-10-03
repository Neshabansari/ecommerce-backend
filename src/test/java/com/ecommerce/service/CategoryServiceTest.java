package com.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.dto.CategoryRequest;
import com.ecommerce.dto.CategoryResponse;
import com.ecommerce.exception.ConflictException;
import com.ecommerce.exception.DuplicateResourceException;
import com.ecommerce.model.Category;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;
    @InjectMocks private CategoryService categoryService;

    @Test
    void create_duplicateNameIgnoringCase_isRejected() {
        when(categoryRepository.existsByNameIgnoreCase("Dining")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CategoryRequest(" Dining ", "x")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void update_sameNameInDifferentCase_isAllowed() {
        Category existing = Category.builder().id(1L).name("Dining Tables").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.update(1L, new CategoryRequest("dining tables", " "));

        assertThat(response.name()).isEqualTo("dining tables");
        assertThat(response.description()).isNull();
    }

    @Test
    void delete_categoryWithProducts_isRejected() {
        Category existing = Category.builder().id(1L).name("Dining Tables").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L)).isInstanceOf(ConflictException.class);

        verify(categoryRepository, never()).delete(any(Category.class));
    }
}