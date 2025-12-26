package reve_back.application.ports.out;

import reve_back.domain.model.LoyaltyTiers;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public interface LoyaltyTiersRepositoryPort {
    BigDecimal findCostByTier(Integer tierLevel);
    Optional<LoyaltyTiers> findByTierLevel(int level);
    boolean existsByTierLevel(int level);
}
