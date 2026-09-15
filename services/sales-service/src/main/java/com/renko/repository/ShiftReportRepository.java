package com.renko.repository;

import com.renko.entities.ShiftReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShiftReportRepository extends JpaRepository<ShiftReportEntity, Long>
{
    List<ShiftReportEntity> findByCashierId(Long id);
    List<ShiftReportEntity> findByStoreId(Long storeId);
    Optional<ShiftReportEntity> findTopByCashierIdAndShiftEndIsNullOrderByShiftStartDesc(Long cashierId);
    Optional<ShiftReportEntity> findByCashierIdAndShiftStartBetween(Long cashierId,
                                                                    LocalDateTime start,
                                                                    LocalDateTime end);
}
