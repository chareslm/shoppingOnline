package com.chareslm.shopping.product.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.product.dto.request.CategoryCreateRequest;
import com.chareslm.shopping.product.dto.request.CategoryUpdateRequest;
import com.chareslm.shopping.product.dto.response.CategoryNodeResponse;
import com.chareslm.shopping.product.dto.response.CategoryResponse;
import com.chareslm.shopping.product.service.CategoryService;
import com.chareslm.shopping.security.context.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** 全量类目树（用户端，公开）。 */
    @GetMapping("/categories/tree")
    public ApiResponse<List<CategoryNodeResponse>> tree() {
        return ApiResponse.success(categoryService.listTree());
    }

    /** 平铺类目列表（管理端）。 */
    @GetMapping("/admin/categories")
    @PreAuthorize("hasAuthority('category:manage')")
    public ApiResponse<List<CategoryResponse>> listAll() {
        return ApiResponse.success(categoryService.listAll());
    }

    @PostMapping("/admin/categories")
    @PreAuthorize("hasAuthority('category:manage')")
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryCreateRequest request) {
        return ApiResponse.success(categoryService.create(CurrentUser.require().userId(), request));
    }

    @PutMapping("/admin/categories/{categoryId}")
    @PreAuthorize("hasAuthority('category:manage')")
    public ApiResponse<CategoryResponse> update(@PathVariable Long categoryId,
                                                @Valid @RequestBody CategoryUpdateRequest request) {
        return ApiResponse.success(categoryService.update(CurrentUser.require().userId(), categoryId, request));
    }

    @DeleteMapping("/admin/categories/{categoryId}")
    @PreAuthorize("hasAuthority('category:manage')")
    public ApiResponse<Void> delete(@PathVariable Long categoryId) {
        categoryService.delete(categoryId);
        return ApiResponse.success(null);
    }
}
