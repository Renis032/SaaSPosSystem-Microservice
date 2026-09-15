package com.renko.mapper;

import com.renko.entities.RefundEntity;
import com.renko.payload.dto.RefundDto;

public class RefundMapper
{
    public static RefundDto toDto(RefundEntity refundEntity)
    {
        return RefundDto.builder()
                .id(refundEntity.getId())
                .orderId(refundEntity.getOrder() != null ? refundEntity.getOrder().getId() : null)
                .reason(refundEntity.getReason())
                .amount(refundEntity.getAmount())
                .shiftReportId(refundEntity.getShiftReportEntity() != null
                        ? refundEntity.getShiftReportEntity().getId()
                        : null)
                .cashierId(refundEntity.getCashierId())
                .cashierName(null)
                .storeId(refundEntity.getStoreId())
                .paymentType(refundEntity.getPaymentType())
                .createdAt(refundEntity.getCreatedAt())
                .updatedAt(refundEntity.getUpdatedAt())
                .build();
    }
}
