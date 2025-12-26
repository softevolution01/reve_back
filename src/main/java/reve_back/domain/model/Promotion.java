package reve_back.domain.model;

import java.time.LocalDate;
import java.util.List;

public record Promotion(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        Boolean isActive,
        String strategyCode,
        List<PromotionRule> rules,
        Integer triggerQuantity
) {
}
