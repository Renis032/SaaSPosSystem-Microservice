package com.renko.repository;

import com.renko.entities.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long>
{
    List<CategoryEntity> findByStoreId(Long storeId);
}
