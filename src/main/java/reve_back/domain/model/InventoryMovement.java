package reve_back.domain.model;

import java.time.LocalDateTime;

public record InventoryMovement(
        Long id,
        Long bottleId,
        Integer quantity,
        String type,      // INGRESO o EGRESO
        String unit,
        String reason,    // COMPRA, AJUSTE, VENTA, DAÑO
        Long userId,
        LocalDateTime createdAt
) {}
