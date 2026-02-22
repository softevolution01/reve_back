package reve_back.application.service;

import org.springframework.stereotype.Service;
import reve_back.domain.model.CartItem;
import reve_back.domain.model.PromotionStrategy;
import reve_back.domain.model.StrategyResult;
import reve_back.infrastructure.persistence.entity.PromotionEntity;
import reve_back.infrastructure.persistence.entity.PromotionRuleEntity;
import reve_back.infrastructure.persistence.enums.global.PromotionRuleType;

import java.math.BigDecimal;
import java.util.*;

@Service
public class SandwichPlusExtraStrategy implements PromotionStrategy {

    @Override
    public String getStrategyCode() {
        // IMPORTANTE: Esto debe ser exactamente igual a lo que dice tu tabla promotions
        return "SANDWICH_PLUS_EXTRA";
    }

    @Override
    public StrategyResult execute(List<CartItem> items, PromotionEntity promotion) {

        // 1. EXTRACCIÓN DE REGLAS DINÁMICAS
        int buyQty = extractRuleValue(promotion, PromotionRuleType.CONFIG_BUY_QUANTITY, 3);
        int payQty = extractRuleValue(promotion, PromotionRuleType.CONFIG_PAY_QUANTITY, 2);
        BigDecimal extraDiscount = extractBigDecimalRule(promotion, PromotionRuleType.EXTRA_DISCOUNT_PERCENT, new BigDecimal("50.00"));

        // 2. SEPARACIÓN DE CANDIDATOS
        List<CartItem> candidates = new ArrayList<>();
        for (CartItem item : items) {
            if (item.allowPromotions() || item.isPromoForced()) {
                candidates.add(item);
            }
        }

        // 3. ORDENAMIENTO DE MAYOR A MENOR PRECIO Y USO DE DEQUE (Doble Cola)
        candidates.sort(Comparator.comparing(CartItem::price).reversed());
        Deque<CartItem> deque = new ArrayDeque<>(candidates);

        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<Long> lockedTempIds = new ArrayList<>();

        int freeQty = buyQty - payQty; // N - M = 1

        // 4. LÓGICA DE CÁLCULO INTELIGENTE (Protege el margen)
        // Ejecutamos mientras haya al menos para formar la base del 3x2
        while (deque.size() >= buyQty) {

            // A) Cobrar los M más caros (Se sacan del inicio de la cola)
            for (int i = 0; i < payQty; i++) {
                CartItem paidItem = deque.pollFirst();
                if (paidItem != null) lockedTempIds.add(paidItem.tempId());
            }

            // B) Regalar el (o los) MÁS BARATOS (Se sacan del final de la cola) -> GRATIS
            for (int i = 0; i < freeQty; i++) {
                CartItem freeItem = deque.pollLast();
                if (freeItem != null) {
                    totalDiscount = totalDiscount.add(freeItem.price());
                    lockedTempIds.add(freeItem.tempId());
                }
            }

            // C) El "Extra" al 50% de descuento (El siguiente más barato, se saca del final)
            if (!deque.isEmpty()) {
                CartItem extraItem = deque.pollLast();
                BigDecimal discountAmount = extraItem.price()
                        .multiply(extraDiscount)
                        .divide(new BigDecimal("100"));

                totalDiscount = totalDiscount.add(discountAmount);
                lockedTempIds.add(extraItem.tempId());
            }
        }

        return new StrategyResult(totalDiscount, lockedTempIds, getStrategyCode());
    }

    private int extractRuleValue(PromotionEntity promotion, PromotionRuleType type, int defaultValue) {
        if (promotion == null || promotion.getRules() == null) {
            return defaultValue;
        }

        return promotion.getRules().stream()
                .filter(r -> r.getRuleType() == type) // Filtra por el tipo de regla (Enum)
                .map(r -> {
                    // Si tienes la columna item_index en tu entidad PromotionRuleEntity
                    if (r.getItemIndex() != null) return r.getItemIndex();

                    // Si usas discount_value para guardar el 3 o el 2
                    if (r.getDiscountValue() != null) return r.getDiscountValue().intValue();

                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(defaultValue);
    }

    // --- MÉTODO PARA EXTRAER DECIMALES (Ej: 50.00%) ---
    private BigDecimal extractBigDecimalRule(PromotionEntity promotion, PromotionRuleType type, BigDecimal defaultValue) {
        if (promotion == null || promotion.getRules() == null) {
            return defaultValue;
        }

        return promotion.getRules().stream()
                .filter(r -> r.getRuleType() == type)
                .map(PromotionRuleEntity::getDiscountValue) // Aquí usamos tu clase PromotionRuleEntity directamente
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(defaultValue);
    }
}
