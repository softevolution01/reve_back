package reve_back.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

public record ContractItemRequest(
        @NotNull Long productId,
        @NotNull Integer quantity
) {}
