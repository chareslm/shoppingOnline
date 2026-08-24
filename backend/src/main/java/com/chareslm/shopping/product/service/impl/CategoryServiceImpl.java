package com.chareslm.shopping.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.product.dto.request.CategoryCreateRequest;
import com.chareslm.shopping.product.dto.request.CategoryUpdateRequest;
import com.chareslm.shopping.product.dto.response.CategoryNodeResponse;
import com.chareslm.shopping.product.dto.response.CategoryResponse;
import com.chareslm.shopping.product.entity.Category;
import com.chareslm.shopping.product.mapper.CategoryMapper;
import com.chareslm.shopping.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryNodeResponse> listTree() {
        List<Category> all = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getLevel)
                        .orderByAsc(Category::getSortOrder));
        Map<Long, List<CategoryNodeResponse>> byParent = all.stream()
                .map(this::toNode)
                .collect(Collectors.groupingBy(CategoryNodeResponse::parentId));
        return buildChildren(0L, byParent);
    }

    @Override
    public List<CategoryResponse> listAll() {
        return categoryMapper.selectList(
                        new LambdaQueryWrapper<Category>()
                                .orderByAsc(Category::getLevel)
                                .orderByAsc(Category::getSortOrder))
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public CategoryResponse create(Long operatorId, CategoryCreateRequest request) {
        Long parentId = request.parentId() == null ? 0L : request.parentId();
        int level = 1;
        if (parentId != 0) {
            Category parent = requireCategory(parentId);
            if (parent.getLevel() != null && parent.getLevel() >= 3) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
            level = (parent.getLevel() == null ? 1 : parent.getLevel()) + 1;
        }
        Category category = new Category();
        category.setParentId(parentId);
        category.setName(request.name().trim());
        category.setLevel(level);
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setIcon(request.icon());
        category.setStatus(request.status() == null ? 1 : request.status());
        categoryMapper.insert(category);
        return toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse update(Long operatorId, Long categoryId, CategoryUpdateRequest request) {
        Category category = requireCategory(categoryId);
        category.setName(request.name().trim());
        category.setLevel(request.level() == null ? category.getLevel() : request.level());
        category.setSortOrder(request.sortOrder() == null ? category.getSortOrder() : request.sortOrder());
        category.setIcon(request.icon());
        category.setStatus(request.status() == null ? category.getStatus() : request.status());
        categoryMapper.updateById(category);
        return toResponse(category);
    }

    @Override
    @Transactional
    public void delete(Long categoryId) {
        requireCategory(categoryId);
        long children = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getParentId, categoryId));
        if (children > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_CHILDREN);
        }
        categoryMapper.deleteById(categoryId);
    }

    private Category requireCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    private List<CategoryNodeResponse> buildChildren(Long parentId, Map<Long, List<CategoryNodeResponse>> byParent) {
        return byParent.getOrDefault(parentId, new ArrayList<>()).stream()
                .sorted(Comparator.comparing(CategoryNodeResponse::sortOrder))
                .map(node -> new CategoryNodeResponse(node.id(), node.parentId(), node.name(), node.level(),
                        node.sortOrder(), node.icon(), node.status(), buildChildren(node.id(), byParent)))
                .toList();
    }

    private CategoryNodeResponse toNode(Category category) {
        return new CategoryNodeResponse(category.getId(), category.getParentId(), category.getName(),
                category.getLevel(), category.getSortOrder(), category.getIcon(), category.getStatus(), List.of());
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getParentId(), category.getName(),
                category.getLevel(), category.getSortOrder(), category.getIcon(), category.getStatus());
    }
}
