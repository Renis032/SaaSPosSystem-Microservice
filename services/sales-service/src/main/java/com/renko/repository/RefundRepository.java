package com.renko.repository;

import com.renko.entities.RefundEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RefundRepository extends JpaRepository<RefundEntity, Long>
{
    List<RefundEntity> findByCashierIdAndCreatedAtBetween(Long cashierId, LocalDateTime from, LocalDateTime to);
    List<RefundEntity> findByCashierId(Long id);
    List<RefundEntity> findByShiftReportEntity_Id(Long id);
    List<RefundEntity> findByStoreId(Long storeId);
}
