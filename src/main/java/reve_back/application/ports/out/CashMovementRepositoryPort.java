package reve_back.application.ports.out;

import reve_back.domain.model.CashMovement;

public interface CashMovementRepositoryPort {
    void save(CashMovement cashMovement);
}