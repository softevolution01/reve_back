package reve_back.infrastructure.mapper;

import org.springframework.stereotype.Component;
import reve_back.domain.model.Promotion;
import reve_back.domain.model.PromotionRule;
import reve_back.infrastructure.persistence.entity.PromotionEntity;
import reve_back.infrastructure.persistence.entity.PromotionRuleEntity;
import reve_back.infrastructure.persistence.enums.global.PromotionRuleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class PromotionDtoMapper {
    public Promotion toDomain(PromotionEntity entity) {
        if (entity == null) return null;

        // 1. Prevenimos lista nula
        List<PromotionRuleEntity> rules = entity.getRules() != null ? entity.getRules() : new ArrayList<>();

        // 2. Lógica Híbrida: Busca en itemIndex, si no hay, usa discountValue (el 3.00)
        int calculatedTriggerQty = rules.stream()
                .filter(r -> r != null && r.getRuleType() == PromotionRuleType.CONFIG_BUY_QUANTITY)
                .map(r -> {
                    // Intento A: Leer columna item_index
                    if (r.getItemIndex() != null) {
                        return r.getItemIndex();
                    }
                    // Intento B: Leer columna discount_value (3.00 -> 3)
                    if (r.getDiscountValue() != null) {
                        return r.getDiscountValue().intValue();
                    }
                    return null;
                })
                .filter(Objects::nonNull) // Filtra nulos para evitar el Crash
                .findFirst()
                .orElse(3); // Si no encuentra nada (ni 3.00 ni index), asume 3 por defecto

        return new Promotion(
                entity.getId(),
                entity.getName(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getIsActive(),
                entity.getStrategyCode(),
                rules.stream()
                        .map(r -> new PromotionRule(r.getRuleType(), r.getDiscountValue()))
                        .collect(Collectors.toList()),
                calculatedTriggerQty
        );
    }
}
