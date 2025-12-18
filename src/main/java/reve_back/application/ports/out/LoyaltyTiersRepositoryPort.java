package reve_back.application.ports.out;

import java.util.Map;

public interface LoyaltyTiersRepositoryPort {
    Double findCostByTier(Integer tierLevel);
    Map<Integer, Double> findAllAsMap();
}
