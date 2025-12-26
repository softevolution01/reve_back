package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

public record LoyaltyResponse(
        Long clientId,
        Integer currentTier,
        Integer pointsInTier,
        BigDecimal accumulatedMoney,
        BigDecimal costOfNextPoint,
        Boolean isVip
) {}
