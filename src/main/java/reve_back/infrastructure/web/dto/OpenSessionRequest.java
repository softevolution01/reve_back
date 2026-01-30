package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

public record OpenSessionRequest(
        Long branchId,
        Long userId,
        BigDecimal initialAmount
) {}
