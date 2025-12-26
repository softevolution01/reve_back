package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.LoyaltyTiersRepositoryPort;
import reve_back.domain.model.LoyaltyTiers;
import reve_back.infrastructure.mapper.LoyaltyTierDtoMapper;
import reve_back.infrastructure.persistence.entity.LoyaltyTiersEntity;
import reve_back.infrastructure.persistence.jpa.SpringLoyaltyTiersRepository;

import java.math.BigDecimal;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class JpaLoyaltyTierAdapter implements LoyaltyTiersRepositoryPort {

    private final SpringLoyaltyTiersRepository springLoyaltyTiersRepository;
    private final LoyaltyTierDtoMapper mapper;

    @Override
    public BigDecimal findCostByTier(Integer tierLevel) {
        return springLoyaltyTiersRepository.findByTierLevel(tierLevel)
                .map(LoyaltyTiersEntity::getCostPerPoint)
                .orElseThrow(() -> new RuntimeException("No existe configuración para el Nivel " + tierLevel));
    }

    @Override
    public Optional<LoyaltyTiers> findByTierLevel(int level) {
        return springLoyaltyTiersRepository.findByTierLevel(level)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByTierLevel(int level) {
        return springLoyaltyTiersRepository.existsByTierLevel(level);
    }
}
