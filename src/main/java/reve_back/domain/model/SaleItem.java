package reve_back.domain.model;

import java.math.BigDecimal;

public record SaleItem(
        Long id,

        // Referencias
        Long productId,     // Si es botella
        Long decantPriceId, // Si es decant

        // Datos descriptivos (Para mostrar en tickets sin buscar en la tabla productos)
        String productName,
        String productBrand,

        // Datos Operativos
        Integer quantity,
        BigDecimal unitPrice,     // Precio lista original

        // --- AUDITORÍA DE PROMOCIONES (Nombres idénticos a SaleItemEntity) ---
        BigDecimal systemDiscount,       // Descuento automático (3x2)
        BigDecimal manualDiscount,       // Descuento manual del vendedor
        BigDecimal finalSubtotal,        // Resultado final por línea
        Integer volumeMlPerUnit,

        Boolean isPromoLocked,           // ¿Bloqueado por 3x2?
        Boolean isPromoForced,           // ¿Forzado por vendedor?
        String promoStrategyApplied      // "3X2_PROFIT", "NONE"
) {}