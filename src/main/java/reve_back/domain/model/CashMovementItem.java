package reve_back.domain.model;

import java.math.BigDecimal;

public record CashMovementItem(
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
