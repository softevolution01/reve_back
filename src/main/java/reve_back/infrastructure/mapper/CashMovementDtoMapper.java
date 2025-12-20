package reve_back.infrastructure.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reve_back.domain.model.CashMovement;
import reve_back.infrastructure.persistence.entity.CashMovementEntity;

@RequiredArgsConstructor
@Component
public class CashMovementDtoMapper {
    public CashMovementEntity toEntity(CashMovement domain) {
        CashMovementEntity entity = new CashMovementEntity();
        entity.setId(domain.id());
        entity.setAmount(domain.amount());
        entity.setType(domain.type());
        entity.setCreatedAt(domain.createdAt());
        return entity;
    }
}
