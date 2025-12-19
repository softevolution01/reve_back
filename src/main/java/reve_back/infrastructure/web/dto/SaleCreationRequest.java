package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record SaleCreationRequest(
        Long branchId,
        Long userId,
        Long clientId, // Puede ser null
        Boolean applyPromotion, // El interruptor maestro
        BigDecimal manualDiscount, // Descuento "cariño"

        List<SaleItemRequest> items,
        List<PaymentRequest> payments
) {
}
