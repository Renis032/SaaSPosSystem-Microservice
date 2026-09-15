package com.renko.service;

import com.renko.exceptions.UserException;

public interface StoreAccessService
{
    void requireStoreAccess(Long storeId) throws UserException;

    Long requireCurrentStoreId() throws UserException;

    boolean isPlatformAdmin() throws UserException;
}
