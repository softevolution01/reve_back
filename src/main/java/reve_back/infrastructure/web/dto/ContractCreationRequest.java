package reve_back.infrastructure.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ContractCreationRequest(
        @NotNull Long clientId,
        @NotNull Long userId,
        @NotNull Long branchId,
        @NotEmpty List<ContractItemRequest> items,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotNull BigDecimal discount,
        @NotNull BigDecimal advancePayment, // Adelanto
        @NotNull Long paymentMethodId
) {
}
