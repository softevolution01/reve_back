package reve_back.infrastructure.web.dto;

public record LoyaltyResponse(
        Long clientId,
        Integer currentTier,
        Integer pointsInTier,
        Double accumulatedMoney,
        Double costOfNextPoint,
        boolean isVip
) {}
