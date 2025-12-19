package reve_back.domain.model;

import java.math.BigDecimal;

public record PromotionRule(
        Long id,
        String ruleType, // NTH_ITEM_FREE, etc.
        Integer itemIndex,
        BigDecimal discountPercentage
) {
}
