package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record SaleCreationRequest(
        Long branchId,
        Long userId,
        Long clientId,
        BigDecimal systemDiscount,

        List<SaleItemRequest> items,
        List<PaymentRequest> payments
) {
}
