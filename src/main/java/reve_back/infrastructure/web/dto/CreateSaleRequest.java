package reve_back.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import reve_back.domain.model.CartItem;

import java.math.BigDecimal;
import java.util.List;

public record CreateSaleRequest(
        List<CartItemRequest> items,
        Long promotionId,
        @NotNull PaymentDetailsRequest paymentDetails // 👇 El objeto clave
) {
    public record PaymentDetailsRequest(
            @NotNull String method, // "EFECTIVO", "TARJETA", "MIXTO"
            PaymentBreakdownRequest breakdown
    ) {}

    public record PaymentBreakdownRequest(
            BigDecimal efectivo,
            BigDecimal yape,
            BigDecimal tarjeta
    ) {}
}
