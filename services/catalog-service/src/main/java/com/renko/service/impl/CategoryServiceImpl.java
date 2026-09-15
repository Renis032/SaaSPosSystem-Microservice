package com.renko.service.impl;

import com.renko.entities.CategoryEntity;
import com.renko.exceptions.ExceptionMessages;
import com.renko.mapper.CategoryMapper;
import com.renko.payload.dto.CategoryDto;
import com.renko.repository.CategoryRepository;
import com.renko.service.CategoryService;
import com.renko.service.StoreAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService
{
    private final CategoryRepository categoryRepository;
    private final StoreAccessService storeAccessService;

    @Override
    public CategoryDto createCategoryDto(CategoryDto categoryDto) throws Exception
    {
        if(categoryDto.getStoreId() == null)
        {
            throw ExceptionMessages.required("storeId", "storeId is required to create a category");
        }
        storeAccessService.requireStoreAccess(categoryDto.getStoreId());
        CategoryEntity saved = categoryRepository.save(CategoryEntity.builder()
                .name(categoryDto.getName())
                .storeId(categoryDto.getStoreId())
                .build());
        return CategoryMapper.toDto(saved);
    }

    @Override
    public CategoryDto getCategoryById(Long id) throws Exception
    {
        return CategoryMapper.toDto(categoryRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Category", id)));
    }

    @Override
    public List<CategoryDto> getAllCategories()
    {
        return categoryRepository.findAll().stream().map(CategoryMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<CategoryDto> getCategoriesByStore(Long storeId) throws Exception
    {
        storeAccessService.requireStoreAccess(storeId);
        return categoryRepository.findByStoreId(storeId).stream().map(CategoryMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) throws Exception
    {
        CategoryEntity existing = categoryRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Category", id, "update"));
        if(categoryDto.getName() != null) existing.setName(categoryDto.getName());
        if(categoryDto.getStoreId() != null)
        {
            storeAccessService.requireStoreAccess(categoryDto.getStoreId());
            existing.setStoreId(categoryDto.getStoreId());
        }
        return CategoryMapper.toDto(categoryRepository.save(existing));
    }

    @Override
    public void deleteCategory(Long id) throws Exception
    {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Category", id, "delete"));
        categoryRepository.delete(category);
    }

    @Override
    public void deleteAllCategories()
    {
        categoryRepository.deleteAll();
    }
}
