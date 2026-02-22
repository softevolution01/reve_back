package reve_back.application.service;

import org.springframework.stereotype.Service;
import reve_back.domain.model.*;

import java.math.BigDecimal;
import java.util.*;

@Service
public class SandwichNxMPlus50Strategy implements PromotionStrategy {

    @Override
    public String getStrategyCode() {
        return "SANDWICH_N_M_PLUS_50"; // Nuevo código para la BD
    }

    @Override
    public StrategyResult execute(List<CartItem> items, int n, int m) {
        List<CartItem> candidates = new ArrayList<>();

        // 1. Filtrar asegurando que solo sean DECANTS (isEligibleForPromo valida esto)
        for (CartItem item : items) {
            if (item.isEligibleForPromo()) {
                candidates.add(item);
            }
        }

        candidates.sort(Comparator.comparing(CartItem::price).reversed());
        Deque<CartItem> deque = new ArrayDeque<>(candidates);

        BigDecimal totalDiscount = BigDecimal.ZERO;
        ArrayList<Long> lockedTempIds = new ArrayList<>();

        boolean appliedAtLeastOneNxM = false;

        // 2. Aplicar la lógica base del 3x2 (NxM)
        while (deque.size() >= n) {
            appliedAtLeastOneNxM = true;

            // Cobrar los M más caros
            for (int i = 0; i < m; i++) {
                CartItem paidItem = deque.pollFirst();
                if (paidItem != null) lockedTempIds.add(paidItem.tempId());
            }

            // Regalar (N-M) más baratos
            int freeItemsCount = n - m;
            for (int i = 0; i < freeItemsCount; i++) {
                CartItem freeItem = deque.pollLast();
                if (freeItem != null) {
                    totalDiscount = totalDiscount.add(freeItem.price());
                    lockedTempIds.add(freeItem.tempId());
                }
            }
        }

        // 3. NUEVA LÓGICA: El 4to (el siguiente sobrante) al 50%
        // Solo se activa si se aplicó al menos un 3x2 y aún sobran ítems.
        if (appliedAtLeastOneNxM && !deque.isEmpty()) {
            // Sacamos el más caro de los sobrantes (está al frente del deque)
            CartItem fourthItem = deque.pollFirst();
            if (fourthItem != null) {
                // Se aplica el 50% de descuento
                BigDecimal halfDiscount = fourthItem.price().multiply(new BigDecimal("0.5"));
                totalDiscount = totalDiscount.add(halfDiscount);
                // Se bloquea para que no reciba descuento manual
                lockedTempIds.add(fourthItem.tempId());
            }
        }

        return new StrategyResult(totalDiscount, lockedTempIds, getStrategyCode());
    }
}