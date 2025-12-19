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

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal discount;

    @Column(name = "extra_discount", precision = 10, scale = 2)
    private BigDecimal extraDiscount;

    @Builder.Default
    private String status = "VENDIDO";

    // [CHIVATO] Auditoría de fuerza bruta
    @Column(name = "is_promo_forced")
    @Builder.Default
    private Boolean isPromoForced = false;
}
