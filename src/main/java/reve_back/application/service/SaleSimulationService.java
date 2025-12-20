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
import java.util.List;

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
            promoResult = new StrategyResult(BigDecimal.ZERO, List.of(), "NONE");
        }

        // 2. Consolidar resultados finales (Funciona igual para ambos casos)
        return buildResponse(cartItems, promoResult);
    }

    private SaleSimulationResponse buildResponse(List<CartItem> items, StrategyResult promoResult) {
        BigDecimal manualDiscountTotal = BigDecimal.ZERO;
        BigDecimal totalBruto = BigDecimal.ZERO;

        // 1. Descuento automático (Estrategia)
        BigDecimal systemDiscount = promoResult.totalDiscount();

        // 2. Recorremos items para calcular totales y descuentos manuales
        for (CartItem item : items) {
            totalBruto = totalBruto.add(item.price());

            // ¿El ítem está libre? (NO fue usado por la promo automática)
            if (!promoResult.lockedTempItemIds().contains(item.tempId())) {

                // A. Null Safety: Si viene null del front, lo tratamos como 0
                BigDecimal itemManualDiscount = item.manualDiscount() != null
                        ? item.manualDiscount()
                        : BigDecimal.ZERO;

                // B. Validación Lógica: Solo sumamos si es mayor a 0
                if (itemManualDiscount.compareTo(BigDecimal.ZERO) > 0) {

                    // C. REGLA DE NEGOCIO: El descuento no puede ser mayor al precio del producto.
                    // Si el producto vale 50 y el vendedor puso descuento 100, solo descontamos 50.
                    BigDecimal applicableDiscount = itemManualDiscount.min(item.price());

                    manualDiscountTotal = manualDiscountTotal.add(applicableDiscount);
                }
            }
        }

        // 3. Totales Finales
        BigDecimal totalDiscount = systemDiscount.add(manualDiscountTotal);

        // Cálculo final: Bruto - Descuentos
        BigDecimal finalAmount = totalBruto.subtract(totalDiscount);

        // D. Seguridad Final: El total nunca puede ser negativo
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        return new SaleSimulationResponse(
                totalDiscount,           // Suma de ambos descuentos
                systemDiscount,          // Solo Promo Automática
                manualDiscountTotal,     // Solo Manual
                finalAmount,             // A Pagar
                promoResult.lockedTempItemIds(),
                promoResult.strategyCode(),
                "Simulación calculada exitosamente"
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