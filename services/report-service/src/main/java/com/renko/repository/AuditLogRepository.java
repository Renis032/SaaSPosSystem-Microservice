package com.renko.repository;

import com.renko.entities.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long>
{
    List<AuditLogEntity> findTop100ByStoreIdOrderByCreatedAtDesc(Long storeId);
}
