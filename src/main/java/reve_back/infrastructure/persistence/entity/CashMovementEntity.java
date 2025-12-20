package reve_back.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_movements")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashMovementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "type", nullable = false)
    private String type; // 'INGRESO', 'EGRESO', 'APERTURA', 'CIERRE'

    @Column(columnDefinition = "TEXT")
    private String description;

    // Auditoría: ¿Quién registró el movimiento?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by")
    private UserEntity registeredBy;

    // Enlace opcional a la venta (si fue un ingreso por venta)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private SaleEntity sale;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
