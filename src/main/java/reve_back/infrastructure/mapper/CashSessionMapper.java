package reve_back.infrastructure.mapper;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reve_back.domain.model.CashSession;
import reve_back.infrastructure.persistence.entity.CashSessionEntity;
import reve_back.infrastructure.persistence.entity.UserEntity;
import reve_back.infrastructure.persistence.entity.WarehouseEntity;

@Component
@RequiredArgsConstructor
public class CashSessionMapper {

    private final EntityManager entityManager;

    // 1. Convertir de ENTIDAD (BD) a DOMINIO (Lógica)
    public CashSession toDomain(CashSessionEntity entity) {
        if (entity == null) {
            return null;
        }

        return CashSession.builder()
                .id(entity.getId())
                .warehouseId(entity.getWarehouse().getId())

                // Extraemos solo el ID del usuario (Relación -> Long)
                .openedByUserId(entity.getOpenedByUser() != null ? entity.getOpenedByUser().getId() : null)
                .openedAt(entity.getOpenedAt())
                .initialCash(entity.getInitialCash())

                .closedByUserId(entity.getClosedByUser() != null ? entity.getClosedByUser().getId() : null)
                .closedAt(entity.getClosedAt())

                .finalCashExpected(entity.getFinalCashExpected())
                .finalCashCounted(entity.getFinalCashCounted())
                .difference(entity.getDifference())
                .totalManualIncome(entity.getTotalManualIncome())
                .totalManualExpense(entity.getTotalManualExpense())
                .notes(entity.getNotes())

                // Convertimos el Enum o String de la BD al Enum del Dominio
                .status(entity.getStatus())
                .build();
    }

    // 2. Convertir de DOMINIO (Lógica) a ENTIDAD (BD)
    public CashSessionEntity toEntity(CashSession domain) {
        if (domain == null) {
            return null;
        }

        CashSessionEntity entity = new CashSessionEntity();

        entity.setId(domain.getId());
        entity.setWarehouse(entityManager.getReference(WarehouseEntity.class, domain.getWarehouseId()));

        entity.setOpenedAt(domain.getOpenedAt());
        entity.setInitialCash(domain.getInitialCash());

        entity.setClosedAt(domain.getClosedAt());
        entity.setFinalCashExpected(domain.getFinalCashExpected());
        entity.setFinalCashCounted(domain.getFinalCashCounted());
        entity.setDifference(domain.getDifference());
        entity.setTotalManualIncome(domain.getTotalManualIncome());
        entity.setTotalManualExpense(domain.getTotalManualExpense());
        entity.setNotes(domain.getNotes());
        entity.setStatus(domain.getStatus());


        if (domain.getOpenedByUserId() != null) {
            entity.setOpenedByUser(entityManager.getReference(UserEntity.class, domain.getOpenedByUserId()));
        }

        if (domain.getClosedByUserId() != null) {
            entity.setClosedByUser(entityManager.getReference(UserEntity.class, domain.getClosedByUserId()));
        }

        return entity;
    }
}