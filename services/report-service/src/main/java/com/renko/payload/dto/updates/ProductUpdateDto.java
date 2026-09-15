package com.renko.payload.dto.updates;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductUpdateDto
{
    private String name;
    private String sku;
    private String description;
    private Double maxRetailPrice;
    private Double sellingPrice;
    private String brand;
    private String imageUrl;
    private Long categoryId;
}
