package reve_back.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record Sale(
        Long id,
        LocalDateTime saleDate,

        // Relaciones (Solo IDs para mantener el modelo ligero)
        Long branchId,
        Long userId,
        Long clientId,

        // Nombres descriptivos (opcionales, útiles para no hacer joins extra en consultas simples)
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
    // Validación de lógica financiera (Opcional pero recomendado)
    // Esto te ayuda a detectar errores si la base de datos se corrompe
    public boolean isFinanciallyConsistent() {
        // Calculado: (Bruto - Descuento + Recargo)
        BigDecimal calculated = totalAmount.subtract(totalDiscount).add(paymentSurcharge);

        // Compara con lo guardado (usando compareTo para evitar errores de decimales)
        return calculated.compareTo(totalFinalCharged) == 0;
    }
}
