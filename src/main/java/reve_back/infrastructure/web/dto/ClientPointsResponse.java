package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

public record ClientPointsResponse(
        Long clientId,
        String fullname,
        Boolean isVip,
        BigDecimal montoHistoricoTotal,
        BigDecimal montoTotalPostVip,
        BigDecimal montoAcumuladoCiclo,
        Integer ciclosCompletados
) {
}
