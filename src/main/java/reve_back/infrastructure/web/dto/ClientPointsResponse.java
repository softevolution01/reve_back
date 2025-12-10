package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

public record ClientPointsResponse(
        Long clientId,
        String fullname,
        boolean isVip,
        BigDecimal montoHistoricoTotal,
        BigDecimal montoTotalPostVip,
        BigDecimal montoAcumuladoCiclo,
        int ciclosCompletados
) {
}
