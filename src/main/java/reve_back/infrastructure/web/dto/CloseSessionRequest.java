package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

public record CloseSessionRequest(
        Long branchId,
        Long userId,
        BigDecimal countedCash,
        String notes
) {}
