package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import reve_back.domain.model.CashMovement;
import reve_back.infrastructure.persistence.entity.CashMovementEntity;

public interface SpringDataCashMovementRepository extends JpaRepository<CashMovementEntity, Long> {
    void save(CashMovement cashMovement);
}
