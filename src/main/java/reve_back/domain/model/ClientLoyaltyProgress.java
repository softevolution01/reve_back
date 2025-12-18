package reve_back.domain.model;

import java.time.LocalDateTime;

public record ClientLoyaltyProgress(
        Long clientId,
        Integer currentTier,
        Integer pointsInTier,
        Double accumulatesMoney,
        LocalDateTime updateAt
) {

}
