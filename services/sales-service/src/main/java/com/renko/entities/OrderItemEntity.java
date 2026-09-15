package com.renko.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
public class OrderItemEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    private Integer quantity;
    private Double price;
    private Double originalPrice;
    private Double discountApplied = 0.0;

    /** Snapshot fields for receipts (product lives in catalog-service). */
    private String productName;
    private String productSku;
    private Double productDiscountPercentage;

    @Column(name = "product_id")
    private Long productId;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private OrderEntity orderEntity;
}
