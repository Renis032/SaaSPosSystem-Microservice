package com.renko.repository;

import com.renko.entities.CustomerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long>
{
    List<CustomerEntity> findByStoreId(Long storeId);
    List<CustomerEntity> findByPhoneContainingIgnoreCase(String phone);
    List<CustomerEntity> findByFullNameContainingIgnoreCase(String name);

    @Query("""
            SELECT c FROM CustomerEntity c
            WHERE c.storeId = :storeId
              AND (
                    :q IS NULL
                    OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(c.phone, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(c.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
            """)
    Page<CustomerEntity> searchByStore(@Param("storeId") Long storeId,
                                       @Param("q") String q,
                                       Pageable pageable);
}
