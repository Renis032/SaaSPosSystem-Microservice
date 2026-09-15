package com.renko.service;

import com.renko.payload.dto.CategoryDto;

import java.util.List;

public interface CategoryService
{
    CategoryDto createCategoryDto(CategoryDto categoryDto) throws Exception;
    CategoryDto getCategoryById(Long id) throws Exception;
    List<CategoryDto> getAllCategories();
    List<CategoryDto> getCategoriesByStore(Long storeId) throws Exception;
    CategoryDto updateCategory(Long id, CategoryDto categoryDto) throws Exception;
    void deleteCategory(Long id) throws Exception;
    void deleteAllCategories();
}
