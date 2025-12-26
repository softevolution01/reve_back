package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public record SaleSimulationResponse(
        BigDecimal totalDiscount,      // Descuento total (System + Manual)
        BigDecimal systemDiscount,     // Solo lo que regaló la estrategia 3x2
        BigDecimal manualDiscountTotal,// Suma de los descuentos manuales en soles
        BigDecimal finalAmount,        // Monto neto a cobrar
        ArrayList<Long> appliedToItemIds,   // IDs de decants bloqueados
        String strategyApplied,
        String message
) {}
