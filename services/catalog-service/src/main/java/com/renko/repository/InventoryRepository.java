package com.renko.repository;

import com.renko.entities.InventoryEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryEntity, Long>
{
    InventoryEntity findByStoreIdAndProductEntity_Id(Long storeId, Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryEntity i WHERE i.storeId = :storeId AND i.productEntity.id = :productId")
    Optional<InventoryEntity> findByStoreAndProductForUpdate(@Param("storeId") Long storeId,
                                                             @Param("productId") Long productId);

    List<InventoryEntity> findByStoreId(Long storeId);

    @Query("SELECT i FROM InventoryEntity i WHERE i.storeId = :storeId AND i.quantity <= i.lowStockThreshold")
    List<InventoryEntity> findLowStockByStoreId(@Param("storeId") Long storeId);
}
