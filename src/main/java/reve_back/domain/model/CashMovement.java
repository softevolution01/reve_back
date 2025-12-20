package reve_back.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CashMovement(
        Long id,
        Long branchId,

        BigDecimal amount,
        String type,            // 'INGRESO', 'EGRESO', 'APERTURA', 'CIERRE'
        String description,

        Long registeredBy,      // ID del usuario
        Long saleId,            // ID de la venta (si aplica)

        LocalDateTime createdAt
) {}
