package reve_back.application.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reve_back.application.ports.in.SaleSimulationUseCase;
import reve_back.application.ports.out.PromotionRepositoryPort;
import reve_back.domain.model.*;
import reve_back.infrastructure.persistence.entity.PromotionEntity;
import reve_back.infrastructure.persistence.entity.PromotionRuleEntity;
import reve_back.infrastructure.persistence.enums.global.PromotionRuleType;
import reve_back.infrastructure.persistence.jpa.SpringDataPromotionRepository;
import reve_back.infrastructure.web.dto.CartItemRequest;
import reve_back.infrastructure.web.dto.SaleSimulationRequest;
import reve_back.infrastructure.web.dto.SaleSimulationResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleSimulationService implements SaleSimulationUseCase {
    private final PromotionStrategyFactory strategyFactory;
    private final SpringDataPromotionRepository springDataPromotionRepository;

    public SaleSimulationResponse calculateSimulation(List<CartItem> cartItems, Long promotionId) {

        StrategyResult promoResult;

        // 1. Verificamos si hay una promoción activa
        if (promotionId != null) {
            // Lógica de Promoción (Solo si hay ID)
            PromotionEntity promotion = springDataPromotionRepository.findById(promotionId)
                    .orElseThrow(() -> new EntityNotFoundException("Promoción no encontrada"));

            int n = extractRuleValue(promotion, PromotionRuleType.CONFIG_BUY_QUANTITY, 3); // N
            int m = extractRuleValue(promotion, PromotionRuleType.CONFIG_PAY_QUANTITY, 2); // M

            PromotionStrategy strategy = strategyFactory.getStrategy(promotion.getStrategyCode());
            promoResult = strategy.execute(cartItems, n, m);
        } else {
            // Caso "Sin Descuento" o ID nulo: Retornamos resultado vacío (0 descuento)
            promoResult = new StrategyResult(BigDecimal.ZERO, new ArrayList<>(), "NONE");
        }

        // 2. Consolidar resultados finales (Funciona igual para ambos casos)
        return buildResponse(cartItems, promoResult);
    }

    private SaleSimulationResponse buildResponse(List<CartItem> incomingItems, StrategyResult promoResult) {

        // 1. COPIA MUTABLE
        List<CartItem> items = new ArrayList<>(incomingItems);

        // 2. ORDENAMIENTO (Prioridad a items SIN descuento manual para las promos automáticas)
        items.sort(Comparator.comparing(item ->
                item.manualDiscount() != null ? item.manualDiscount() : BigDecimal.ZERO
        ));

        BigDecimal totalBruto = BigDecimal.ZERO;
        BigDecimal systemDiscount = promoResult.totalDiscount();

        List<Long> lockedItemIds = new ArrayList<>(promoResult.lockedTempItemIds());

        // =========================================================================================
        // NUEVA LÓGICA: 4to DECANT AL 50
        // =========================================================================================
        if (!lockedItemIds.isEmpty()) {
            // A. Buscamos los "sobrantes": Decants activos que NO fueron usados en el 3x2
            List<CartItem> leftoverDecants = items.stream()
                    .filter(item -> item.isEligibleForPromo()) // Solo Decants y habilitados
                    .filter(item -> !lockedItemIds.contains(item.tempId())) // Que no estén ya en el 3x2
                    .sorted(Comparator.comparing(CartItem::price).reversed()) // El más caro de los sobrantes primero
                    .toList();

            // B. Si hay sobrantes (significa que tenemos un 4to, 5to item, etc.)
            if (!leftoverDecants.isEmpty()) {
                // Tomamos el primero de los sobrantes (que sería el 4to item en lógica global)
                CartItem fourthItem = leftoverDecants.get(0);

                // C. Calculamos el 50% de descuento
                BigDecimal halfPriceDiscount = fourthItem.price().multiply(new BigDecimal("0.5"));

                // D. Aplicamos el descuento y bloqueamos el item
                systemDiscount = systemDiscount.add(halfPriceDiscount);
                lockedItemIds.add(fourthItem.tempId());
            }
        }
        // =========================================================================================

        List<CartItem> availableForManual = new ArrayList<>();

        for (CartItem item : items) {
            totalBruto = totalBruto.add(item.price());

            if (lockedItemIds.contains(item.tempId())) {
                // Si el item fue usado en 3x2 O fue el 4to item al 50%, ya no recibe descuento manual
                lockedItemIds.remove(item.tempId()); // removemos para manejar duplicados si los hubiera
            } else {
                // Está libre para descuento manual
                availableForManual.add(item);
            }
        }

        BigDecimal manualDiscountTotal = BigDecimal.ZERO;

        Map<Long, List<CartItem>> itemsGrouped = availableForManual.stream()
                .collect(Collectors.groupingBy(CartItem::tempId));

        for (List<CartItem> group : itemsGrouped.values()) {
            if (group.isEmpty()) continue;

            BigDecimal groupTotalPrice = group.stream()
                    .map(CartItem::price)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal targetDiscount = group.get(0).manualDiscount() != null
                    ? group.get(0).manualDiscount()
                    : BigDecimal.ZERO;

            BigDecimal applicableDiscount = targetDiscount.min(groupTotalPrice);

            manualDiscountTotal = manualDiscountTotal.add(applicableDiscount);
        }

        BigDecimal totalDiscount = systemDiscount.add(manualDiscountTotal);
        BigDecimal finalAmount = totalBruto.subtract(totalDiscount);

        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        return new SaleSimulationResponse(
                totalDiscount,           // Suma de ambos descuentos
                systemDiscount,          // 3x2 + el 50% del 4to item
                manualDiscountTotal,     // Solo Manual
                finalAmount,             // A Pagar
                promoResult.lockedTempItemIds(),
                promoResult.strategyCode(),
                "Simulación calculada exitosamente (Incluye lógica 4to item al 50%)"
        );
    }

    private int extractRuleValue(PromotionEntity promotion, PromotionRuleType type, int defaultValue) {
        return promotion.getRules().stream()
                .filter(r -> r.getRuleType() == type)
                .map(r -> {
                    // 1. Intenta leer el índice entero
                    if (r.getItemIndex() != null) return r.getItemIndex();
                    // 2. Si es nulo, lee el valor decimal (tu caso: 2.00 -> 2)
                    if (r.getDiscountValue() != null) return r.getDiscountValue().intValue();
                    return null;
                })
                .filter(java.util.Objects::nonNull) // Evita NullPointerException
                .findFirst()
                .orElse(defaultValue);
    }
}