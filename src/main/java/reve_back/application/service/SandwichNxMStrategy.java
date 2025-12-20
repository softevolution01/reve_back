package reve_back.application.service;

import org.springframework.stereotype.Service;
import reve_back.domain.model.*;
import reve_back.infrastructure.persistence.enums.global.ItemType;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SandwichNxMStrategy implements PromotionStrategy {

    @Override
    public String getStrategyCode() {
        return "SANDWICH_N_M"; // Identificador en la tabla promotions
    }

    @Override
    public StrategyResult execute(List<CartItem> items, int n, int m) {
        // 1. SEPARACIÓN
        List<CartItem> candidates = new ArrayList<>();
        // ... (tu lógica de filtrado de decants/allowPromotions) ...
        for (CartItem item : items) {
            if (item.allowPromotions() || item.isPromoForced()) {
                candidates.add(item);
            }
        }

        // 2. ORDENAMIENTO (Mayor a Menor)
        candidates.sort(Comparator.comparing(CartItem::price).reversed());
        Deque<CartItem> deque = new ArrayDeque<>(candidates);

        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<Integer> lockedTempIds = new ArrayList<>();

        // 3. CÁLCULO
        while (deque.size() >= n) {

            // A) COBRAR LOS M MÁS CAROS
            for (int i = 0; i < m; i++) {
                CartItem paidItem = deque.pollFirst();
                if (paidItem != null) {
                    // CORRECCIÓN: Aunque se paga, ES PARTE DE LA PROMO.
                    // Lo agregamos a la lista de bloqueados.
                    lockedTempIds.add(paidItem.tempId());
                }
            }

            // B) REGALAR LOS (N-M) MÁS BARATOS
            int freeItemsCount = n - m;
            for (int i = 0; i < freeItemsCount; i++) {
                CartItem freeItem = deque.pollLast();
                if (freeItem != null) {
                    totalDiscount = totalDiscount.add(freeItem.price());
                    lockedTempIds.add(freeItem.tempId()); // Este ya lo tenías
                }
            }
        }

        return new StrategyResult(totalDiscount, lockedTempIds, "SANDWICH_N_M");
    }
}
