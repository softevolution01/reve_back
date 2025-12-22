package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

public record SaleItemRequest(
        Long idInventario,
        String tipoVendible,
        Integer quantity,
        BigDecimal price,

        BigDecimal manualDiscount,
        Boolean forcePromo,
        Long forcePromoId
) {}
