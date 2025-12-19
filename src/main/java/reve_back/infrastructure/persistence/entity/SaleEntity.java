package reve_back.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Setter
@Table(name="sales")
public class SaleEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_date")
    @CreationTimestamp
    private LocalDateTime saleDate;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "igv_rate", precision = 4, scale = 2)
    private BigDecimal igvRate;

    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user; // Vendedor

    @ManyToOne
    @JoinColumn(name = "client_id")
    private ClientEntity client;

    @ManyToOne
    @JoinColumn(name = "promotion_id")
    private PromotionEntity promotion; // Promo cabecera (opcional)

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL)
    private List<SaleItemEntity> items;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL)
    private List<SalePaymentEntity> payments;
}
