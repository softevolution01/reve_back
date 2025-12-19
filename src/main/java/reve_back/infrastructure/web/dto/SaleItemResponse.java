package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

public record SaleItemResponse(
        String productName,
        String type, // "5ml", "10ml", "Botella"
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalLinePrice,
        BigDecimal discountApplied,
        Boolean wasPromoForced
) {}
