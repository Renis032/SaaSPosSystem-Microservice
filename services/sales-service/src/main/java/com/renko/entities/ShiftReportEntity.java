package com.renko.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShiftReportEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private LocalDateTime shiftStart;
    private LocalDateTime shiftEnd;

    private Double totalSales;
    private Double totalRefunds;
    private Double netSales;
    private double totalOrders;

    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "branch_id")
    private Long branchId;

    @Transient
    private List<PaymentSummaryEntity> paymentSummaries;

    @ElementCollection
    @CollectionTable(name = "shift_top_product_ids", joinColumns = @JoinColumn(name = "shift_report_id"))
    @Column(name = "product_id")
    private List<Long> topSellingProductIds;

    @ManyToMany(cascade = CascadeType.MERGE)
    private List<OrderEntity> recentOrders;

    @OneToMany(mappedBy = "shiftReportEntity", cascade = CascadeType.ALL)
    private List<RefundEntity> refunds;
}
