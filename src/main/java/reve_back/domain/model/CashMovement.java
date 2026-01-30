package reve_back.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CashMovement(
        Long id,
        Long sessionId,
        Long branchId,

        BigDecimal amount,
        String type,            // 'INGRESO', 'EGRESO'
        String description,
        String method,

        Long registeredBy,      // ID del usuario
        String registeredByUsername,
        Long saleId,

        LocalDateTime createdAt
) {}
