package com.renko.mapper;

import com.renko.entities.CategoryEntity;
import com.renko.entities.ProductEntity;
import com.renko.payload.dto.ProductDto;

public class ProductMapper
{
    public static ProductDto toDto(ProductEntity productEntity)
    {
        ProductDto productDto = new ProductDto();
        productDto.setFromEntity(productEntity);
        return productDto;
    }

    public static ProductEntity toEntity(ProductDto productDto, Long storeId, CategoryEntity categoryEntity)
    {
        ProductEntity productEntity = new ProductEntity();
        productEntity.setId(productDto.getId());
        productEntity.setName(productDto.getName());
        productEntity.setSku(productDto.getSku());
        productEntity.setDescription(productDto.getDescription());
        productEntity.setMaxRetailPrice(productDto.getMaxRetailPrice());
        productEntity.setSellingPrice(productDto.getSellingPrice());
        productEntity.setBrand(productDto.getBrand());
        productEntity.setImageUrl(productDto.getImageUrl());
        productEntity.setDiscountPercentage(
                productDto.getDiscountPercentage() != null ? productDto.getDiscountPercentage() : 0.0);
        productEntity.setStoreId(storeId);
        productEntity.setCategoryEntity(categoryEntity);
        return productEntity;
    }
}
