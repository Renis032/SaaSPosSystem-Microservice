package com.renko.payload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDto
{
    private Long id;
    private Integer quantity;

    private Long storeId;
    private Long productId;

    private Integer lowStockThreshold;

    private LocalDateTime lastUpdated;
}
