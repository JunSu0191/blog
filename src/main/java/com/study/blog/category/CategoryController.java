package com.study.blog.category;

import com.study.blog.category.dto.CategoryDto;
import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponseTemplate<List<CategoryDto.CategorySummary>>> listCategories() {
        return ApiResponseFactory.ok(categoryService.listCategories());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseTemplate<CategoryDto.CategorySummary>> createCategory(@Valid @RequestBody CategoryDto.UpsertRequest request) {
        CategoryDto.CategorySummary response = categoryService.createCategory(request);
        return ApiResponseFactory.created(URI.create("/api/categories/" + response.id()), response);
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseTemplate<CategoryDto.CategorySummary>> updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryDto.UpsertRequest request) {
        return ApiResponseFactory.ok(categoryService.updateCategory(categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseTemplate<Void>> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ApiResponseFactory.noContent();
    }
}

