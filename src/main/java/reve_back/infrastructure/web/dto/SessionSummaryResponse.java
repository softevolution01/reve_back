package reve_back.infrastructure.web.dto;


import java.math.BigDecimal;

public record SessionSummaryResponse(
        String method,
        BigDecimal amount
) {}
