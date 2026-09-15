package com.renko.service;

import com.renko.domain.StoreStatus;
import com.renko.exceptions.UserException;
import com.renko.entities.StoreEntity;
import com.renko.payload.dto.StoreDto;
import com.renko.payload.dto.UserDto;

import java.util.List;

public interface StoreService
{
    StoreDto createStore(StoreDto storeDto, UserDto admin);
    StoreDto getStoreById(Long id) throws Exception;
    List<StoreEntity> getAllStores();
    StoreEntity getStoreByAdmin() throws UserException;
    StoreDto updateStore(Long id, StoreDto storeDto) throws UserException;
    void deleteStore(Long id) throws UserException;
    StoreDto getStoreByEmployee() throws Exception;
    StoreDto moderateStore(Long id, StoreStatus storeStatus) throws Exception;
    void deleteAllStores() throws UserException;
}
