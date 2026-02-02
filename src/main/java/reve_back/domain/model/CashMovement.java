package reve_back.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CashMovement(
        Long id,
        Long sessionId,
        Long branchId,
        BigDecimal amount,
        String type,
        String description,
        String method,
        Long registeredBy,
        String registeredByName, // Username del vendedor
        Long saleId,
        Long contractId,         // <--- NUEVO: ID de Contrato
        java.time.LocalDateTime createdAt,
        java.util.List<CashMovementItem> items // <--- NUEVO: Lista de productos
) {
}
