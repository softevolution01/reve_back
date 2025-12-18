package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.LoyaltyTiersRepositoryPort;
import reve_back.infrastructure.persistence.entity.LoyaltyTiersEntity;
import reve_back.infrastructure.persistence.jpa.SpringLoyaltyTiersRepository;

import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class JpaLoyaltyTierAdapter implements LoyaltyTiersRepositoryPort {

    private final SpringLoyaltyTiersRepository springLoyaltyTiersRepository;

    @Override
    public Double findCostByTier(Integer tierLevel) {
        return springLoyaltyTiersRepository.findById(tierLevel)
                .map(LoyaltyTiersEntity::getCostPerPoint)
                .orElse(Double.valueOf("60.00"));
    }

    @Override
    public Map<Integer, Double> findAllAsMap() {
        return springLoyaltyTiersRepository.findAll().stream()
                .collect(Collectors.toMap(
                        LoyaltyTiersEntity::getTierLevel,
                        LoyaltyTiersEntity::getCostPerPoint
                ));
    }
}
