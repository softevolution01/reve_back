package reve_back.application.service;

import org.springframework.stereotype.Service;
import reve_back.domain.model.*;
import reve_back.infrastructure.persistence.entity.PromotionEntity;
import reve_back.infrastructure.persistence.enums.global.PromotionRuleType;

import java.math.BigDecimal;
import java.util.*;

@Service
public class SandwichNxMStrategy implements PromotionStrategy {

    @Override
    public String getStrategyCode() {
        return "SANDWICH_N_M"; // Identificador en la tabla promotions
    }

    @Override
    // CAMBIO CLAVE: Ahora recibe PromotionEntity en lugar de int n, int m
    public StrategyResult execute(List<CartItem> items, PromotionEntity promotion) {

        int n = extractRuleValue(promotion, PromotionRuleType.CONFIG_BUY_QUANTITY, 3);
        int m = extractRuleValue(promotion, PromotionRuleType.CONFIG_PAY_QUANTITY, 2);

        List<CartItem> candidates = new ArrayList<>();
        for (CartItem item : items) {
            // Validamos que el ítem participe en promociones o esté forzado
            if (item.allowPromotions() || item.isPromoForced()) {
                candidates.add(item);
            }
        }

        // 3. ORDENAMIENTO (De mayor a menor precio)
        candidates.sort(Comparator.comparing(CartItem::price).reversed());
        Deque<CartItem> deque = new ArrayDeque<>(candidates);

        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<Long> lockedTempIds = new ArrayList<>();

        // 4. LÓGICA DE CÁLCULO (Sándwich)
        while (deque.size() >= n) {

            // A) COBRAR LOS M MÁS CAROS
            for (int i = 0; i < m; i++) {
                CartItem paidItem = deque.pollFirst();
                if (paidItem != null) {
                    // Es parte de la promo, se bloquea para no mezclar con otras
                    lockedTempIds.add(paidItem.tempId());
                }
            }

            // B) REGALAR LOS (N-M) MÁS BARATOS
            int freeItemsCount = n - m;
            for (int i = 0; i < freeItemsCount; i++) {
                CartItem freeItem = deque.pollLast();
                if (freeItem != null) {
                    // Sumamos el precio del ítem regalado al descuento total
                    totalDiscount = totalDiscount.add(freeItem.price());
                    lockedTempIds.add(freeItem.tempId());
                }
            }
        }

        return new StrategyResult(totalDiscount, lockedTempIds, getStrategyCode());
    }

    // --- MÉTODO AUXILIAR PARA EXTRAER LAS REGLAS ---
    private int extractRuleValue(PromotionEntity promotion, PromotionRuleType type, int defaultValue) {
        if (promotion == null || promotion.getRules() == null) {
            return defaultValue;
        }

        return promotion.getRules().stream()
                .filter(r -> r.getRuleType() == type)
                .map(r -> {
                    // Priorizamos el índice si existe, si no, el valor de descuento
                    if (r.getItemIndex() != null) return r.getItemIndex();
                    if (r.getDiscountValue() != null) return r.getDiscountValue().intValue();
                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(defaultValue);
    }
}
