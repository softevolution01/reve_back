package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

public record RegisterMovementRequest(
        Long branchId,
        Long userId,
        String type,        // "INGRESO" o "EGRESO"
        BigDecimal amount,
        String description,
        String method,
        Long saleId,
        Long contractId
) {}
