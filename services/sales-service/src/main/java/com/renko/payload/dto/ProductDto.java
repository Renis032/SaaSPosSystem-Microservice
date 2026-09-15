package com.renko.payload.dto;

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
}
