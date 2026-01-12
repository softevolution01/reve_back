package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

public record LabelItemDTO(
        String nombreCompleto, // VERSACE BRIGHT CRYSTAL
        String detalle,        // DECANT 2ml
        String codigoVisual,   // D0577
        BigDecimal precio      // 49.00
) {}