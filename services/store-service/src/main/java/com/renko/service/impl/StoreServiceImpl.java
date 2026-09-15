package com.renko.service.impl;

import com.renko.client.AuthServiceClient;
import com.renko.client.BillingServiceClient;
import com.renko.domain.StoreStatus;
import com.renko.domain.SubscriptionPlan;
import com.renko.exceptions.ExceptionMessages;
import com.renko.exceptions.UserException;
import com.renko.mapper.StoreMapper;
import com.renko.entities.StoreContactEntity;
import com.renko.entities.StoreEntity;
import com.renko.payload.dto.StoreDto;
import com.renko.payload.dto.UserDto;
import com.renko.repository.StoreRepository;
import com.renko.service.StoreAccessService;
import com.renko.service.StoreService;
import com.renko.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService
{
    private final StoreRepository storeRepository;
    private final UserService userService;
    private final StoreAccessService storeAccessService;
    private final AuthServiceClient authServiceClient;
    private final BillingServiceClient billingServiceClient;

    @Override
    public StoreDto createStore(StoreDto storeDto, UserDto admin)
    {
        StoreEntity storeEntity = StoreMapper.toEntity(storeDto, admin.getId());
        StoreEntity saved = storeRepository.save(storeEntity);

        try
        {
            authServiceClient.linkStore(admin.getId(), saved.getId());
        }
        catch(Exception e)
        {
            System.out.println("Failed to link store on auth-service: " + e.getMessage());
        }

        try
        {
            billingServiceClient.createTrial(saved.getId(), SubscriptionPlan.STARTER);
            saved.setStatus(StoreStatus.ACTIVE);
            saved = storeRepository.save(saved);
        }
        catch(Exception e)
        {
            System.out.println("Failed to create trial on billing-service: " + e.getMessage());
        }

        return StoreMapper.toDto(saved);
    }

    @Override
    public StoreDto getStoreById(Long id) throws Exception
    {
        storeAccessService.requireStoreAccess(id);
        StoreEntity storeEntity = storeRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Store", id));
        return StoreMapper.toDto(storeEntity);
    }

    @Override
    public List<StoreEntity> getAllStores()
    {
        return storeRepository.findAll();
    }

    @Override
    public StoreEntity getStoreByAdmin() throws UserException
    {
        UserDto admin = userService.getCurrentUser();
        return storeRepository.findByStoreAdminId(admin.getId());
    }

    @Override
    public StoreDto updateStore(Long id, StoreDto storeDto) throws UserException
    {
        UserDto currentUser = userService.getCurrentUser();
        StoreEntity store = storeRepository.findByStoreAdminId(currentUser.getId());
        if(store == null)
        {
            throw UserException.withDetails(
                    "You do not have permission to update a store. No store is linked to the current admin user.",
                    Map.of("userId", currentUser.getId(), "requestedStoreId", id)
            );
        }

        store.setBrandName(storeDto.getBrandName());
        store.setDescription(storeDto.getDescription());
        if(storeDto.getStoreType() != null) store.setStoreType(storeDto.getStoreType());
        if(storeDto.getContact() != null)
        {
            store.setContact(StoreContactEntity.builder()
                    .address(storeDto.getContact().getAddress())
                    .phone(storeDto.getContact().getPhone())
                    .email(storeDto.getContact().getEmail())
                    .build());
        }
        return StoreMapper.toDto(storeRepository.save(store));
    }

    @Override
    public void deleteStore(Long id) throws UserException
    {
        storeAccessService.requireStoreAccess(id);
        StoreEntity storeEntity = storeRepository.findById(id)
                .orElseThrow(() -> new UserException("Store not found with id: " + id, Map.of("storeId", id)));
        storeRepository.delete(storeEntity);
    }

    @Override
    public StoreDto getStoreByEmployee() throws Exception
    {
        UserDto currentUser = userService.getCurrentUser();
        if(currentUser.getStoreId() == null)
        {
            throw UserException.withDetails(
                    "Current user is not linked to any store",
                    Map.of("userId", currentUser.getId(), "email", currentUser.getEmail())
            );
        }
        StoreEntity storeEntity = storeRepository.findById(currentUser.getStoreId())
                .orElseThrow(() -> ExceptionMessages.notFound("Store", currentUser.getStoreId(),
                        "load employee store for userId=" + currentUser.getId()));
        return StoreMapper.toDto(storeEntity);
    }

    @Override
    public StoreDto moderateStore(Long id, StoreStatus storeStatus) throws Exception
    {
        StoreEntity storeEntity = storeRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Store", id, "set status to " + storeStatus));
        storeEntity.setStatus(storeStatus);
        return StoreMapper.toDto(storeRepository.save(storeEntity));
    }

    @Override
    public void deleteAllStores() throws UserException
    {
        storeRepository.deleteAll();
    }
}
