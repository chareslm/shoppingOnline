package com.chareslm.shopping.product.service;

import com.chareslm.shopping.product.dto.request.CategoryCreateRequest;
import com.chareslm.shopping.product.dto.request.CategoryUpdateRequest;
import com.chareslm.shopping.product.dto.response.CategoryNodeResponse;
import com.chareslm.shopping.product.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    /** 全量类目树（用户端）。 */
    List<CategoryNodeResponse> listTree();

    /** 平铺类目列表（管理端）。 */
    List<CategoryResponse> listAll();

    CategoryResponse create(Long operatorId, CategoryCreateRequest request);

    CategoryResponse update(Long operatorId, Long categoryId, CategoryUpdateRequest request);

    void delete(Long categoryId);
}
