package com.renko.repository;

import com.renko.entities.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<StoreEntity, Long>
{
    StoreEntity findByStoreAdminId(Long adminId);
}
