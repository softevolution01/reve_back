package reve_back.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record Sale(
        Long id,
        LocalDateTime saleDate,

        Long branchId,
        Long userId,
        Long clientId,
        Long cashSessionId,

        String clientFullname,

        // --- FINANZAS (Nombres idénticos a SaleEntity) ---
        BigDecimal totalAmount,        // Suma bruta de productos (Unit Price * Quantity)
        BigDecimal totalDiscount,      // Suma total de descuentos (System + Manual)
        BigDecimal igvRate,            // 0.18
        BigDecimal paymentSurcharge,   // El 5% extra por tarjeta
        BigDecimal totalFinalCharged,  // Lo que realmente pagó el cliente

        // Configuración
        String paymentMethod,          // "TARJETA", "EFECTIVO", etc.

        // Composición
        List<SaleItem> items,
        List<SalePayment> payments
) {
}
