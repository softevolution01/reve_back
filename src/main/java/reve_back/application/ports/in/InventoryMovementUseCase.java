package reve_back.application.ports.in;

import reve_back.domain.model.InventoryMovement;
import reve_back.infrastructure.web.dto.QuickMovementRequest;

import java.util.List;

public interface InventoryMovementUseCase {

    void processMovement(QuickMovementRequest request);

    List<InventoryMovement> getMovementHistory(Long bottleId);
}
