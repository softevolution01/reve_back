package reve_back.domain.model;

import reve_back.infrastructure.persistence.enums.global.ItemType;

import java.math.BigDecimal;

public record CartItem(
        Integer tempId,             // Identificador único en el carrito (UI)
        Long productId,          // ID del producto padre
        Long decantPriceId,      // ID del precio específico (si es decant)
        BigDecimal price,        // Precio original
        ItemType itemType,       // ENVIADO DESDE EL FRONT: BOTTLE o DECANT
        BigDecimal manualDiscount,
        boolean allowPromotions, // Atributo del producto padre
        boolean isPromoForced,   // Bypass del vendedor (Checkbox "Forzar")
        boolean isPromoLocked    // Estado de bloqueo (para recalculaciones)
) {
    // Lógica simplificada: solo decants permitidos o forzados participan
    public boolean isEligibleForPromo() {
        return itemType == ItemType.DECANT && (allowPromotions || isPromoForced);
    }
}
