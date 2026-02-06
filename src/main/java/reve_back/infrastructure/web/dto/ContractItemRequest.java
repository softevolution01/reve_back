package reve_back.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ContractItemRequest(
        @NotNull Long productId,
        @NotNull Integer quantity,
        BigDecimal extraAmount
) {}
