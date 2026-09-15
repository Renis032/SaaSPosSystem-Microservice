package com.renko.mapper;

import com.renko.entities.BranchEntity;
import com.renko.payload.dto.BranchDto;

import java.util.ArrayList;

public class BranchMapper
{
    public static BranchDto toDto(BranchEntity branchEntity)
    {
        return BranchDto.builder()
                .id(branchEntity.getId())
                .name(branchEntity.getName())
                .address(branchEntity.getAddress())
                .phone(branchEntity.getPhone())
                .email(branchEntity.getEmail())
                .workdays(branchEntity.getWorkdays() != null ? new ArrayList<>(branchEntity.getWorkdays()) : null)
                .openTime(branchEntity.getOpenTime())
                .closeTime(branchEntity.getCloseTime())
                .createdAt(branchEntity.getCreatedAt())
                .updatedAt(branchEntity.getUpdatedAt())
                .storeId(branchEntity.getStoreId())
                .managerId(branchEntity.getManagerId())
                .build();
    }

    public static BranchEntity toEntity(BranchDto branchDto, Long storeId)
    {
        return BranchEntity.builder()
                .id(branchDto.getId())
                .name(branchDto.getName())
                .address(branchDto.getAddress())
                .phone(branchDto.getPhone())
                .email(branchDto.getEmail())
                .storeId(storeId)
                .managerId(branchDto.getManagerId())
                .workdays(branchDto.getWorkdays())
                .openTime(branchDto.getOpenTime())
                .closeTime(branchDto.getCloseTime())
                .createdAt(branchDto.getCreatedAt())
                .updatedAt(branchDto.getUpdatedAt())
                .build();
    }
}
