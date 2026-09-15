package com.renko.mapper;

import com.renko.entities.InventoryEntity;
import com.renko.entities.ProductEntity;
import com.renko.payload.dto.InventoryDto;

public class InventoryMapper
{
    public static InventoryDto toDto(InventoryEntity inventoryEntity)
    {
        return InventoryDto.builder()
                .id(inventoryEntity.getId())
                .quantity(inventoryEntity.getQuantity())
                .storeId(inventoryEntity.getStoreId())
                .productId(inventoryEntity.getProductEntity() != null ? inventoryEntity.getProductEntity().getId() : null)
                .lowStockThreshold(inventoryEntity.getLowStockThreshold())
                .lastUpdated(inventoryEntity.getLastUpdated())
                .build();
    }

    public static InventoryEntity toEntity(InventoryDto inventoryDto, Long storeId, ProductEntity productEntity)
    {
        return InventoryEntity.builder()
                .id(inventoryDto.getId())
                .productEntity(productEntity)
                .storeId(storeId)
                .quantity(inventoryDto.getQuantity())
                .lowStockThreshold(inventoryDto.getLowStockThreshold() != null ? inventoryDto.getLowStockThreshold() : 10)
                .version(0L)
                .lastUpdated(inventoryDto.getLastUpdated())
                .build();
    }
}
