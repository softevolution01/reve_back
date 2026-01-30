package reve_back.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractCreationRequest(
        @NotNull Long clientId,
        @NotNull Long userId,
        @NotNull Long branchId,
        @NotNull Long productId,
        @NotNull Integer quantity,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotNull BigDecimal discount,
        @NotNull BigDecimal advancePayment, // Adelanto
        @NotNull Long paymentMethodId
) {
}
