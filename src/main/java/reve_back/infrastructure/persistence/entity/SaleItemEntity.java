package reve_back.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "sale_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private SaleEntity sale;

    // Puede ser Botella O Decant (Uno será null)
    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    @ManyToOne
    @JoinColumn(name = "decant_price_id")
    private DecantPriceEntity decantPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "system_discount")
    private BigDecimal systemDiscount; // Cuánto descontó el 3x2

    @Column(name = "manual_discount")
    private BigDecimal manualDiscount; // Cuánto bajó el vendedor a mano

    @Column(name = "final_subtotal")
    private BigDecimal finalSubtotal; // (unitPrice * qty) - system - manual

    @Column(name = "is_promo_locked")
    private Boolean isPromoLocked; // true si fue bloqueado por el 3x2

    @Column(name = "is_promo_forced")
    private Boolean isPromoForced; // true si el vendedor activó "Forzar Promo"

    @Column(name = "promo_strategy_applied")
    private String promoStrategyApplied;
}
