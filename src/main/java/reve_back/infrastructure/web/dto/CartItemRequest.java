package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

public record CartItemRequest(
        Long tempId,
        Long productId,
        Long decantPriceId,
        BigDecimal price,
        String itemType, // "BOTTLE" o "DECANT"
        BigDecimal manualDiscount,
        Boolean isPromoForced,
        Boolean allowPromotions
) {}
