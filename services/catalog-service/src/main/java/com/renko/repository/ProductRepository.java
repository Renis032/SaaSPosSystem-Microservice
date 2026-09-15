package com.renko.repository;

import com.renko.entities.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<ProductEntity, Long>
{
    List<ProductEntity> findByStoreId(Long storeId);

    @Query("""
            SELECT p FROM ProductEntity p
            WHERE p.storeId = :storeId
              AND (
                    :q IS NULL
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
            """)
    Page<ProductEntity> searchByStore(@Param("storeId") Long storeId,
                                      @Param("q") String q,
                                      Pageable pageable);
}
