package com.renko.repository;

import com.renko.entities.StoreSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreSubscriptionRepository extends JpaRepository<StoreSubscriptionEntity, Long>
{
    Optional<StoreSubscriptionEntity> findByStoreId(Long storeId);
}
