package com.renko.service.impl;

import com.renko.domain.UserRole;
import com.renko.exceptions.ExceptionMessages;
import com.renko.exceptions.UserException;
import com.renko.payload.dto.UserDto;
import com.renko.service.StoreAccessService;
import com.renko.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoreAccessServiceImpl implements StoreAccessService
{
    private final UserService userService;

    @Override
    public void requireStoreAccess(Long storeId) throws UserException
    {
        if(storeId == null)
        {
            throw ExceptionMessages.required("storeId");
        }

        UserDto user = userService.getCurrentUser();
        if(user.getRole() == UserRole.ADMIN)
        {
            return;
        }

        if(user.getStoreId() == null || false == user.getStoreId().equals(storeId))
        {
            throw UserException.withDetails(
                    "You do not have access to this store",
                    ExceptionMessages.ctx(
                            "requestedStoreId", storeId,
                            "userStoreId", user.getStoreId(),
                            "userId", user.getId(),
                            "role", user.getRole()
                    )
            );
        }
    }

    @Override
    public Long requireCurrentStoreId() throws UserException
    {
        UserDto user = userService.getCurrentUser();
        if(user.getRole() == UserRole.ADMIN)
        {
            throw ExceptionMessages.required(
                    "storeId",
                    "Platform ADMIN must provide an explicit storeId"
            );
        }
        if(user.getStoreId() == null)
        {
            throw ExceptionMessages.required(
                    "storeId",
                    "Current user is not linked to any store. Create or join a store first."
            );
        }
        return user.getStoreId();
    }

    @Override
    public boolean isPlatformAdmin() throws UserException
    {
        return userService.getCurrentUser().getRole() == UserRole.ADMIN;
    }
}
