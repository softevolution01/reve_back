package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

public record WorkerPerformanceResponse(
        String workerName,
        String period,
        Long ticketsCount,
        BigDecimal totalSold
) {}
