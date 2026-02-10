package reve_back.infrastructure.mapper;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reve_back.domain.model.CashMovement;
import reve_back.domain.model.CashMovementItem;
import reve_back.infrastructure.persistence.entity.*;
import reve_back.infrastructure.persistence.enums.global.CashMovementType;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CashMovementDtoMapper {

    private final EntityManager entityManager;

    public CashMovementEntity toEntity(CashMovement domain) {
        CashMovementEntity entity = new CashMovementEntity();

        entity.setId(domain.id());
        entity.setAmount(domain.amount());
        entity.setDescription(domain.description());
        entity.setCreatedAt(domain.createdAt());
        entity.setMethod(domain.method());

        if (domain.type() != null) {
            entity.setType(CashMovementType.valueOf(domain.type()));
        }

        // Relaciones Simples
        if (domain.branchId() != null)
            entity.setBranch(entityManager.getReference(BranchEntity.class, domain.branchId()));

        if (domain.registeredBy() != null)
            entity.setRegisteredBy(entityManager.getReference(UserEntity.class, domain.registeredBy()));

        if (domain.sessionId() != null)
            entity.setCashSession(entityManager.getReference(CashSessionEntity.class, domain.sessionId()));

        // --- RELACIONES DE NEGOCIO ---
        if (domain.saleId() != null) {
            entity.setSale(entityManager.getReference(SaleEntity.class, domain.saleId()));
        }

        // NUEVO: Seteamos el contrato si existe
        if (domain.contractId() != null) {
            entity.setContract(entityManager.getReference(ContractEntity.class, domain.contractId()));
        }

        return entity;
    }

    public CashMovement toDomain(CashMovementEntity entity) {
        if (entity == null) return null;

        // LÓGICA DE EXTRACCIÓN DE ITEMS
        List<CashMovementItem> items = new ArrayList<>();

        // OPCIÓN A: VIENE DE UNA VENTA
        if (entity.getSale() != null && entity.getSale().getItems() != null) {
            items = entity.getSale().getItems().stream()
                    .map(item -> {
                        // 1. Obtener producto
                        var p = item.getProduct();
                        if (p == null) return new CashMovementItem("Producto desconocido", item.getQuantity(), item.getUnitPrice(), item.getFinalSubtotal());

                        // 2. Extraer ML del DecantPrice (si existe)
                        String volumeStr = "";
                        if (item.getDecantPrice() != null && item.getDecantPrice().getVolumeMl() != null) {
                            volumeStr = item.getDecantPrice().getVolumeMl() + "ml";
                        }

                        // 3. Construir Nombre Completo (Marca + Linea + Conc + ML)
                        // Usamos String.join y replaceAll para evitar dobles espacios si algún campo es null
                        String fullName = String.join(" ",
                                p.getBrand(),
                                p.getLine() != null ? p.getLine() : "",
                                p.getConcentration() != null ? p.getConcentration() : "",
                                volumeStr
                        ).trim().replaceAll("\\s+", " "); // Elimina espacios extra

                        return new CashMovementItem(
                                fullName,
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getFinalSubtotal()
                        );
                    })
                    .toList();
        }
        // OPCIÓN B: VIENE DE UN CONTRATO
        else if (entity.getContract() != null && entity.getContract().getItems() != null) {
            items = entity.getContract().getItems().stream()
                    .map(item -> {
                        var p = item.getProduct();
                        if (p == null) return new CashMovementItem("Producto desconocido", item.getQuantity(), item.getUnitPrice(), item.getSubtotal());

                        // En contratos asumimos que no hay DecantPrice (o lo agregas si tu entidad ContractItem lo tiene)
                        String fullName = String.join(" ",
                                p.getBrand(),
                                p.getLine() != null ? p.getLine() : "",
                                p.getConcentration() != null ? p.getConcentration() : ""
                        ).trim().replaceAll("\\s+", " ");

                        return new CashMovementItem(
                                fullName,
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getSubtotal()
                        );
                    })
                    .toList();
        }

        return new CashMovement(
                entity.getId(),
                entity.getCashSession() != null ? entity.getCashSession().getId() : null,
                entity.getBranch() != null ? entity.getBranch().getId() : null,
                entity.getAmount(),
                entity.getType().name(),
                entity.getDescription(),
                entity.getMethod(),
                entity.getRegisteredBy() != null ? entity.getRegisteredBy().getId() : null,
                entity.getRegisteredBy() != null ? entity.getRegisteredBy().getUsername() : "Desconocido",

                // IDs de Referencia
                entity.getSale() != null ? entity.getSale().getId() : null,
                entity.getContract() != null ? entity.getContract().getId() : null, // <--- ID Contrato

                entity.getCreatedAt(),
                items
        );
    }
}
