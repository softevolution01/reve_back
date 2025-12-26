package reve_back.infrastructure.mapper;

import org.springframework.stereotype.Component;
import reve_back.domain.model.LoyaltyTiers;
import reve_back.infrastructure.persistence.entity.LoyaltyTiersEntity;

@Component
public class LoyaltyTierDtoMapper {

    public LoyaltyTiers toDomain(LoyaltyTiersEntity entity) {
        if (entity == null) {
            return null;
        }

        return new LoyaltyTiers(
                entity.getId(),
                entity.getTierLevel(),
                entity.getCostPerPoint()
        );
    }

    public LoyaltyTiersEntity toEntity(LoyaltyTiers domain) {
        if (domain == null) {
            return null;
        }

        LoyaltyTiersEntity entity = new LoyaltyTiersEntity();
        entity.setId(domain.id());
        entity.setTierLevel(domain.tierLevel());
        entity.setCostPerPoint(domain.costPerPoint());
        return entity;
    }
}
