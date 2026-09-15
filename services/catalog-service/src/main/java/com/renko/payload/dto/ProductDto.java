package com.renko.payload.dto;

import com.renko.entities.ProductEntity;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductDto
{
    private Long id;
    private String name;
    private String sku;
    private String description;
    private Double maxRetailPrice;
    private Double sellingPrice;
    private String brand;
    private String imageUrl;
    private Long categoryId;
    private Long storeId;
    private Double discountPercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void setFromEntity(ProductEntity productEntity)
    {
        this.id = productEntity.getId();
        this.name = productEntity.getName();
        this.sku = productEntity.getSku();
        this.description = productEntity.getDescription();
        this.maxRetailPrice = productEntity.getMaxRetailPrice();
        this.sellingPrice = productEntity.getSellingPrice();
        this.brand = productEntity.getBrand();
        this.imageUrl = productEntity.getImageUrl();
        this.discountPercentage = productEntity.getDiscountPercentage();
        this.createdAt = productEntity.getCreatedAt();
        this.updatedAt = productEntity.getUpdatedAt();
        this.storeId = productEntity.getStoreId();
        this.categoryId = productEntity.getCategoryEntity() != null ? productEntity.getCategoryEntity().getId() : null;
    }
}
