package com.renko.payload.dto.updates;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class InventoryUpdateDto
{
    private Integer quantity;

    private Long storeId;
    private Long productId;

    private Integer lowStockThreshold;

    private LocalDateTime lastUpdated;
}
