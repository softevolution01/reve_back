package reve_back.application.ports.out;

import reve_back.domain.model.InventoryMovement;

import java.util.List;

public interface InventoryMovementRepositoryPort {
    InventoryMovement save(InventoryMovement movement);
    List<InventoryMovement> findAllByBottleId(Long bottleId);
}
