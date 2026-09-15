package com.renko.payload.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InventoryDto
{
    private Long id;
    private Integer quantity;
    private Long storeId;
    private Long productId;
    private Integer lowStockThreshold;
    private LocalDateTime lastUpdated;
}
