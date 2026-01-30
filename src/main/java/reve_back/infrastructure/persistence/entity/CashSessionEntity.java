package reve_back.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import reve_back.infrastructure.persistence.enums.global.SessionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cash_sessions")
public class CashSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // VINCULACIÓN AL ALMACÉN FÍSICO (El dueño del dinero)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private WarehouseEntity warehouse;

    // AUDITORÍA DE APERTURA
    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opened_by_user_id", nullable = false)
    private UserEntity openedByUser;

    @Column(name = "initial_cash", nullable = false)
    private BigDecimal initialCash;

    // AUDITORÍA DE CIERRE
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_user_id")
    private UserEntity closedByUser;

    @Column(name = "final_cash_expected")
    private BigDecimal finalCashExpected;

    @Column(name = "final_cash_counted")
    private BigDecimal finalCashCounted;

    @Column(name = "difference")
    private BigDecimal difference;

    @Column(name = "total_manual_income")
    private BigDecimal totalManualIncome;

    @Column(name = "total_manual_expense")
    private BigDecimal totalManualExpense;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    // RELACIONES INVERSAS (Opcional, útil si necesitas sacar reporte completo desde la sesión)
    @OneToMany(mappedBy = "cashSession", fetch = FetchType.LAZY)
    private List<CashMovementEntity> movements;

    @OneToMany(mappedBy = "cashSession", fetch = FetchType.LAZY)
    private List<SaleEntity> sales;
}
