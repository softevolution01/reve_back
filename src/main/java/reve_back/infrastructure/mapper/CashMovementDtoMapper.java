package reve_back.infrastructure.mapper;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reve_back.domain.model.CashMovement;
import reve_back.infrastructure.persistence.entity.*;
import reve_back.infrastructure.persistence.enums.global.CashMovementType;

@Component
@RequiredArgsConstructor
public class CashMovementDtoMapper {

    private final EntityManager entityManager;

    // 1. DOMAIN (Record) -> ENTITY (JPA)
    public CashMovementEntity toEntity(CashMovement domain) {
        CashMovementEntity entity = new CashMovementEntity();

        // Campos simples
        entity.setId(domain.id());
        entity.setAmount(domain.amount());
        entity.setDescription(domain.description());
        entity.setCreatedAt(domain.createdAt());

        if (domain.type() != null) {
            entity.setType(CashMovementType.valueOf(domain.type()));
        }

        // --- RELACIONES EXISTENTES ---
        if (domain.branchId() != null) {
            entity.setBranch(entityManager.getReference(BranchEntity.class, domain.branchId()));
        }

        if (domain.registeredBy() != null) {
            entity.setRegisteredBy(entityManager.getReference(UserEntity.class, domain.registeredBy()));
        }

        if (domain.saleId() != null) {
            entity.setSale(entityManager.getReference(SaleEntity.class, domain.saleId()));
        }

        if (domain.sessionId() != null) {
            entity.setCashSession(entityManager.getReference(CashSessionEntity.class, domain.sessionId()));
        }
        entity.setMethod(domain.method());

        return entity;
    }

    // 2. ENTITY (JPA) -> DOMAIN (Record)
    // Este método lo necesitas para cuando consultes el Dashboard y quieras devolver la lista de movimientos
    public CashMovement toDomain(CashMovementEntity entity) {
        if (entity == null) return null;

        return new CashMovement(
                entity.getId(),

                // Mapeo seguro de nulos para IDs
                entity.getCashSession() != null ? entity.getCashSession().getId() : null,
                entity.getBranch() != null ? entity.getBranch().getId() : null,

                entity.getAmount(),
                entity.getType().name(), // Convertimos Enum a String
                entity.getDescription(),
                entity.getMethod(),

                entity.getRegisteredBy() != null ? entity.getRegisteredBy().getId() : null,
                // Extraemos el username para mostrarlo en el frontend sin query extra
                entity.getRegisteredBy() != null ? entity.getRegisteredBy().getUsername() : "Desconocido",

                entity.getSale() != null ? entity.getSale().getId() : null,
                entity.getCreatedAt()
        );
    }
}
