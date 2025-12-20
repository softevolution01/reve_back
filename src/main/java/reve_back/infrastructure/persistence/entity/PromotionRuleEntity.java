package reve_back.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import reve_back.infrastructure.persistence.enums.global.PromotionRuleType;

import java.math.BigDecimal;

@Entity
@Table(name = "promotion_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionRuleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private PromotionEntity promotion;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private PromotionRuleType ruleType;

    @Column(name = "item_index")
    private Integer itemIndex; // Ej: 3 (para el 3ro gratis)

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    public boolean isBuyQuantity() {
        return ruleType == PromotionRuleType.CONFIG_BUY_QUANTITY;
    }

    public boolean isPayQuantity() {
        return ruleType == PromotionRuleType.CONFIG_PAY_QUANTITY;
    }
}