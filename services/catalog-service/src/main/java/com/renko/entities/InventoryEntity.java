package com.renko.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_inventory_store_product",
                                             columnNames = {"store_id", "product_id"}))
public class InventoryEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity productEntity;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer lowStockThreshold = 10;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate()
    {
        lastUpdated = LocalDateTime.now();
    }

    public boolean isLowStock()
    {
        return quantity != null && lowStockThreshold != null && quantity <= lowStockThreshold;
    }
}
