package reve_back.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Setter
@Table(name="sales")
@Builder
public class SaleEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_date")
    @CreationTimestamp
    private LocalDateTime saleDate;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "total_discount")
    private BigDecimal totalDiscount;

    @Column(name = "igv_rate", precision = 4, scale = 2)
    private BigDecimal igvRate;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_surcharge")
    private BigDecimal paymentSurcharge; // El 5% extra (si aplica)

    @Column(name = "total_final_charged")
    private BigDecimal totalFinalCharged;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user; // Vendedor

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private ClientEntity client;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SaleItemEntity> items = new ArrayList<>();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL)
    private List<SalePaymentEntity> payments;
}
