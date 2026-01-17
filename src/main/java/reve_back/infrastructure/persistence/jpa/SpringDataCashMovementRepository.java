package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reve_back.domain.model.CashMovement;
import reve_back.infrastructure.persistence.entity.CashMovementEntity;

@RepositoryRestResource(exported = false)
public interface SpringDataCashMovementRepository extends JpaRepository<CashMovementEntity, Long> {
    void save(CashMovement cashMovement);
}
