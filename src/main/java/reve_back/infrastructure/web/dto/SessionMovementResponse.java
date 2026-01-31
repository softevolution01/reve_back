package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

public record SessionMovementResponse(
        Long id,
        String time,        // "08:05"
        String type,        // "INGRESO", "VENTA", "EGRESO"
        String description, // "Venta #101..."
        BigDecimal amount,
        String method       // "EFECTIVO", "VISA"
) {}
