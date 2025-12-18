package reve_back.application.ports.out;

import reve_back.infrastructure.persistence.entity.CashMovementEntity;

public interface CashMovementRepositoryPort {
    CashMovementEntity save(CashMovementEntity movement);
}
