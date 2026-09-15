package com.renko.mapper;

import com.renko.entities.OrderEntity;
import com.renko.payload.dto.OrderDto;
import com.renko.payload.dto.OrderItemDto;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OrderMapper
{
    public static OrderDto toDto(OrderEntity orderEntity)
    {
        List<OrderItemDto> itemsFromEntity = orderEntity.getItems() == null
                ? Collections.emptyList()
                : orderEntity.getItems().stream().filter(Objects::nonNull)
                        .map(item -> OrderItemMapper.toDto(item, orderEntity.getId()))
                        .collect(Collectors.toList());

        return OrderDto.builder()
                .id(orderEntity.getId())
                .totalAmount(orderEntity.getTotalAmount())
                .subtotal(orderEntity.getSubtotal())
                .totalDiscount(orderEntity.getTotalDiscount())
                .taxRate(orderEntity.getTaxRate())
                .taxAmount(orderEntity.getTaxAmount())
                .orderDiscountPercent(orderEntity.getOrderDiscountPercent())
                .createdAt(orderEntity.getCreatedAt())
                .updatedAt(orderEntity.getUpdatedAt())
                .storeId(orderEntity.getStoreId())
                .branchId(orderEntity.getBranchId())
                .customerId(orderEntity.getCustomerEntity() != null ? orderEntity.getCustomerEntity().getId() : null)
                .customerName(orderEntity.getCustomerEntity() != null ? orderEntity.getCustomerEntity().getFullName() : null)
                .customerPhone(orderEntity.getCustomerEntity() != null ? orderEntity.getCustomerEntity().getPhone() : null)
                .cashierId(orderEntity.getCashierId())
                .paymentType(orderEntity.getPaymentType())
                .stripePaymentIntentId(orderEntity.getStripePaymentIntentId())
                .items(itemsFromEntity)
                .build();
    }
}
