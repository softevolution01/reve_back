package reve_back.application.ports.out;

import reve_back.domain.model.CashSession;
import reve_back.infrastructure.persistence.entity.CashSessionEntity;

import java.util.Optional;

public interface CashSessionRepositoryPort {

    CashSession save(CashSession cashSession);

    // Buscar sesión activa por Almacén (Warehouse)
    Optional<CashSession> findOpenSessionByWarehouse(Long warehouseId);

    // Validar si existe (para no abrir dos veces)
    boolean existsOpenSessionByWarehouse(Long warehouseId);

    Optional<CashSession> findById(Long id);
}
