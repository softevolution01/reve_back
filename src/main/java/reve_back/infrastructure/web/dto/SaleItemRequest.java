package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

public record SaleItemRequest(
        Long idInventario, // Puede ser ID de Product O de DecantPrice
        String tipoVendible, // "Botella" o "Decant"
        Integer quantity,
        BigDecimal price, // Precio unitario al momento de la venta

        // Lógica de "Cubetas"
        Boolean forcePromo, // True si el vendedor fuerza la entrada
        Long forcePromoId   // ID de la promo específica si hay conflicto (Opcional)
) {}
