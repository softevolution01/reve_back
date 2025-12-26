package reve_back.infrastructure.mapper;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reve_back.domain.model.CashMovement;
import reve_back.infrastructure.persistence.entity.BranchEntity;
import reve_back.infrastructure.persistence.entity.CashMovementEntity;
import reve_back.infrastructure.persistence.entity.SaleEntity;
import reve_back.infrastructure.persistence.entity.UserEntity;

@RequiredArgsConstructor
@Component
public class CashMovementDtoMapper {

    private final EntityManager entityManager;

    public CashMovementEntity toEntity(CashMovement domain) {
        CashMovementEntity entity = new CashMovementEntity();

        entity.setId(domain.id());
        entity.setAmount(domain.amount());
        entity.setType(domain.type());
        entity.setDescription(domain.description());
        entity.setCreatedAt(domain.createdAt());

        if (domain.branchId() != null) {
            entity.setBranch(entityManager.getReference(BranchEntity.class, domain.branchId()));
        }

        if (domain.registeredBy() != null) {
            entity.setRegisteredBy(entityManager.getReference(UserEntity.class, domain.registeredBy()));
        }

        if (domain.saleId() != null) {
            entity.setSale(entityManager.getReference(SaleEntity.class, domain.saleId()));
        }

        return entity;
    }
}
