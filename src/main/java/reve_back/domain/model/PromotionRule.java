package reve_back.domain.model;

import reve_back.infrastructure.persistence.enums.global.PromotionRuleType;

import java.math.BigDecimal;

public record PromotionRule(
        PromotionRuleType ruleType, // Usamos tu Enum directamente
        BigDecimal discountValue
) {
}
