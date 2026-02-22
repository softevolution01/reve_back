package reve_back.application.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reve_back.application.ports.in.SaleSimulationUseCase;
import reve_back.domain.model.*;
import reve_back.infrastructure.persistence.entity.PromotionEntity;
import reve_back.infrastructure.persistence.enums.global.PromotionRuleType;
import reve_back.infrastructure.persistence.jpa.SpringDataPromotionRepository;
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

        if (promotionId != null) {
            PromotionEntity promotion = springDataPromotionRepository.findById(promotionId)
                    .orElseThrow(() -> new EntityNotFoundException("Promoción no encontrada"));

            // DELEGAMOS TODO A LA ESTRATEGIA:
            // Buscamos qué clase Java va a resolver esto (ej: SANDWICH_PLUS_EXTRA)
            PromotionStrategy strategy = strategyFactory.getStrategy(promotion.getStrategyCode());

            // Ahora le pasamos la promoción entera, para que la estrategia saque sus propias reglas
            promoResult = strategy.execute(cartItems, promotion);
        } else {
            promoResult = new StrategyResult(BigDecimal.ZERO, new ArrayList<>(), "NONE");
        }

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
        List<CartItem> availableForManual = new ArrayList<>();

        for (CartItem item : items) {
            totalBruto = totalBruto.add(item.price());

            if (lockedItemIds.contains(item.tempId())) {
                lockedItemIds.remove(item.tempId());
            } else {
                // Está libre
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
                totalDiscount,
                systemDiscount,
                manualDiscountTotal,
                finalAmount,
                promoResult.lockedTempItemIds(),
                promoResult.strategyCode(),
                "Simulación calculada exitosamente"
        );
    }

}