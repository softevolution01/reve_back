package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

public record ProductContractLookupResponse(
        Long productId,
        String fullName, // Marca + Linea
        BigDecimal price,
        Integer currentStock // Para validar en el front
) {
}
