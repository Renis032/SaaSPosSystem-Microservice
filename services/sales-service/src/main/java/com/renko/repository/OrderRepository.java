package com.renko.repository;

import com.renko.entities.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long>
{
    List<OrderEntity> findByCustomerEntity_Id(Long customerId);
    List<OrderEntity> findByStoreId(Long storeId);
    List<OrderEntity> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    List<OrderEntity> findByCashierId(Long cashierId);
    List<OrderEntity> findByStoreIdAndCreatedAtBetween(Long storeId, LocalDateTime from, LocalDateTime to);
    List<OrderEntity> findByCashierIdAndCreatedAtBetween(Long cashierId, LocalDateTime from, LocalDateTime to);
    List<OrderEntity> findTopFiveByStoreIdOrderByCreatedAtDesc(Long storeId);

    long countByStoreId(Long storeId);

    @Query("SELECT SUM(o.totalAmount) FROM OrderEntity o WHERE o.storeId = :storeId")
    Double sumTotalAmountByStoreId(@Param("storeId") Long storeId);

    @Query("""
            SELECT o FROM OrderEntity o
            LEFT JOIN o.customerEntity c
            WHERE o.storeId = :storeId
              AND (
                    :q IS NULL
                    OR CAST(o.id AS string) LIKE CONCAT('%', :q, '%')
                    OR LOWER(COALESCE(c.fullName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(c.phone, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
            """)
    Page<OrderEntity> searchByStore(@Param("storeId") Long storeId,
                                    @Param("q") String q,
                                    Pageable pageable);

    @Query("""
            SELECT DISTINCT o FROM OrderEntity o
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH o.customerEntity
            WHERE o.id = :id
            """)
    Optional<OrderEntity> findDetailedById(@Param("id") Long id);
}
