package com.renko.repository;

import com.renko.entities.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchRepository extends JpaRepository<BranchEntity, Long>
{
    List<BranchEntity> findByStoreId(Long storeId);
    List<BranchEntity> findByManagerId(Long managerId);
}
