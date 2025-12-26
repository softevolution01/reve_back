package reve_back.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClientLoyaltyProgress(
        Long clientId,
        Integer currentTier,
        Integer pointsInTier,
        BigDecimal accumulatedMoney, // <--- CAMBIO CRÍTICO
        LocalDateTime updatedAt
) {

}
