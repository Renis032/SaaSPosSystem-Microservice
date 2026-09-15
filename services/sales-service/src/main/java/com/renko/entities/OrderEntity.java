package com.renko.entities;

import com.renko.domain.PaymentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@Table(name = "orders")
public class OrderEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    private Double totalAmount;
    private Double subtotal;
    private Double totalDiscount = 0.0;
    private Double taxRate = 0.0;
    private Double taxAmount = 0.0;
    private Double orderDiscountPercent = 0.0;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "cashier_id")
    private Long cashierId;

    @OneToMany(mappedBy = "orderEntity", cascade = CascadeType.ALL)
    private List<OrderItemEntity> items;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CustomerEntity customerEntity;

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    private String stripePaymentIntentId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate()
    {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate()
    {
        updatedAt = LocalDateTime.now();
    }
}
