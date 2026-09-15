package com.renko.mapper;

import com.renko.entities.StoreEntity;
import com.renko.payload.dto.StoreDto;

public class StoreMapper
{
    public static StoreDto toDto(StoreEntity storeEntity)
    {
        StoreDto storeDto = new StoreDto();
        storeDto.setFromEntity(storeEntity);
        return storeDto;
    }

    public static StoreEntity toEntity(StoreDto storeDto, Long storeAdminId)
    {
        StoreEntity storeEntity = new StoreEntity();
        storeEntity.setFromDto(storeDto);
        storeEntity.setStoreAdminId(storeAdminId);
        return storeEntity;
    }
}
