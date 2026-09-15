package com.renko.mapper;

import com.renko.entities.OrderItemEntity;
import com.renko.payload.dto.OrderItemDto;

public class OrderItemMapper
{
    public static OrderItemDto toDto(OrderItemEntity orderItemEntity)
    {
        Long orderId = orderItemEntity.getOrderEntity() != null ? orderItemEntity.getOrderEntity().getId() : null;
        return toDto(orderItemEntity, orderId);
    }

    public static OrderItemDto toDto(OrderItemEntity orderItemEntity, Long orderId)
    {
        return OrderItemDto.builder()
                .id(orderItemEntity.getId())
                .quantity(orderItemEntity.getQuantity())
                .price(orderItemEntity.getPrice())
                .originalPrice(orderItemEntity.getOriginalPrice())
                .discountApplied(orderItemEntity.getDiscountApplied())
                .productId(orderItemEntity.getProductId())
                .orderId(orderId)
                .build();
    }
}
