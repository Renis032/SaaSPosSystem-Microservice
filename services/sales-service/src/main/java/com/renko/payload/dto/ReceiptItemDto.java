package com.renko.payload.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReceiptItemDto
{
    private String name;
    private String sku;

    private Integer quantity;

    private Double originalPrice;
    private Double discountPercentage;
    private Double discountAmount;

    private Double finalPrice;

    private Double lineTotal;
}
