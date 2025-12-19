package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.Map;

public record DailyReportResponse(
        BigDecimal saldoCajaMaestra,
        Map<String, BigDecimal> ventasPorSede,
        Map<String, BigDecimal> ventasPorMedioPago
) {
}
