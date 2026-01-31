package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

public record WorkerRankingResponse(
        String workerName,
        Long ticketsCount,
        BigDecimal totalSold
) {}
