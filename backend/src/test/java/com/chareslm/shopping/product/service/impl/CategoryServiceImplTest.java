package com.chareslm.shopping.product.service.impl;

import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.product.dto.request.CategoryCreateRequest;
import com.chareslm.shopping.product.entity.Category;
import com.chareslm.shopping.product.mapper.CategoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoryServiceImplTest {
    private CategoryMapper categoryMapper;
    private CategoryServiceImpl service;

    @BeforeEach
    void setUp() {
        categoryMapper = mock(CategoryMapper.class);
        service = new CategoryServiceImpl(categoryMapper);
    }

    @Test
    void createChildUsesParentLevelPlusOne() {
        Category parent = new Category();
        parent.setId(2L);
        parent.setLevel(1);
        when(categoryMapper.selectById(2L)).thenReturn(parent);
        when(categoryMapper.insert(any(Category.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Category.class).setId(3L);
            return 1;
        });

        var created = service.create(1L, new CategoryCreateRequest(2L, "Phone", 9, 0, null, 1));
        assertEquals(2, created.level());
        assertEquals("Phone", created.name());
    }

    @Test
    void createRejectsFourthLevel() {
        Category parent = new Category();
        parent.setId(9L);
        parent.setLevel(3);
        when(categoryMapper.selectById(9L)).thenReturn(parent);
        assertThrows(BusinessException.class,
                () -> service.create(1L, new CategoryCreateRequest(9L, "TooDeep", 1, 0, null, 1)));
    }
}
