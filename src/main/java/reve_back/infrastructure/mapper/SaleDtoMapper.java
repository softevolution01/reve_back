package reve_back.infrastructure.mapper;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reve_back.domain.model.Sale;
import reve_back.domain.model.SaleItem;
import reve_back.domain.model.SalePayment;
import reve_back.infrastructure.persistence.entity.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SaleDtoMapper {

    private final EntityManager entityManager; // Necesario para getReference (Foreign Keys)

    // =========================================================================
    // 1. TO DOMAIN (Entity -> Model) :: LECTURA
    // =========================================================================
    public Sale toDomain(SaleEntity entity) {
        if (entity == null) return null;

        return new Sale(
                entity.getId(),
                entity.getSaleDate(),
                entity.getBranch() != null ? entity.getBranch().getId() : null,
                entity.getUser() != null ? entity.getUser().getId() : null,
                entity.getClient() != null ? entity.getClient().getId() : null,

                entity.getCashSession() != null ? entity.getCashSession().getId() : null,


                entity.getClient() != null ? entity.getClient().getFullname() : "Cliente Casual",

                // Finanzas
                entity.getTotalAmount(),
                entity.getTotalDiscount(),
                entity.getIgvRate(),
                entity.getPaymentSurcharge(),
                entity.getTotalFinalCharged(),

                entity.getPaymentMethod(),

                // Listas
                toItemDomainList(entity.getItems()),
                toPaymentDomainList(entity.getPayments())
        );
    }

    private List<SaleItem> toItemDomainList(List<SaleItemEntity> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream().map(this::toItemDomain).collect(Collectors.toList());
    }

    private SaleItem toItemDomain(SaleItemEntity entity) {
        return new SaleItem(
                entity.getId(),
                entity.getProduct() != null ? entity.getProduct().getId() : null,
                entity.getDecantPrice() != null ? entity.getDecantPrice().getId() : null,
                buildProductName(entity),
                extractBrand(entity),
                entity.getQuantity(),
                entity.getUnitPrice(),
                // Auditoría
                entity.getSystemDiscount(),
                entity.getManualDiscount(),
                entity.getFinalSubtotal(),
                getVolume(entity),
                entity.getIsPromoLocked() != null && entity.getIsPromoLocked(),
                entity.getIsPromoForced() != null && entity.getIsPromoForced(),
                entity.getPromoStrategyApplied()
        );
    }

    private List<SalePayment> toPaymentDomainList(List<SalePaymentEntity> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream().map(e -> new SalePayment(
                e.getId(),
                e.getPaymentMethod().getId(),
                e.getPaymentMethod().getName(),
                e.getAmount(),
                e.getCommissionApplied()
        )).collect(Collectors.toList());
    }

    private Integer getVolume(SaleItemEntity entity) {
        if (entity.getDecantPrice() != null) {
            return entity.getDecantPrice().getVolumeMl();
        }
        if (entity.getProduct() != null) {
            return entity.getProduct().getVolumeProductsMl();
        }
        return 0;
    }

    // =========================================================================
    // 2. TO ENTITY (Model -> Entity) :: ESCRITURA
    // =========================================================================
    public SaleEntity toEntity(Sale domain) {
        if (domain == null) return null;

        // A. Cabecera (Datos simples)
        SaleEntity entity = SaleEntity.builder()
                .id(domain.id())
                .saleDate(domain.saleDate())
                .totalAmount(domain.totalAmount())
                .totalDiscount(domain.totalDiscount())
                .igvRate(domain.igvRate())
                .paymentSurcharge(domain.paymentSurcharge())
                .totalFinalCharged(domain.totalFinalCharged())
                .paymentMethod(domain.paymentMethod())
                .build();

        // B. Relaciones (Foreign Keys con Proxy - Optimización)
        if (domain.branchId() != null) {
            entity.setBranch(entityManager.getReference(BranchEntity.class, domain.branchId()));
        }
        if (domain.userId() != null) {
            entity.setUser(entityManager.getReference(UserEntity.class, domain.userId()));
        }
        if (domain.clientId() != null) {
            entity.setClient(entityManager.getReference(ClientEntity.class, domain.clientId()));
        }

        if (domain.cashSessionId() != null) {
            entity.setCashSession(entityManager.getReference(CashSessionEntity.class, domain.cashSessionId()));
        }
        if (domain.items() != null) {
            List<SaleItemEntity> itemEntities = domain.items().stream()
                    .map(itemDomain -> toItemEntity(itemDomain, entity))
                    .collect(Collectors.toList());
            entity.setItems(itemEntities);
        }

        // D. Pagos Mixtos (Lista)
        if (domain.payments() != null) {
            List<SalePaymentEntity> paymentEntities = domain.payments().stream()
                    .map(paymentDomain -> toPaymentEntity(paymentDomain, entity))
                    .collect(Collectors.toList());
            entity.setPayments(paymentEntities);
        }

        return entity;
    }

    // Helper para Item Entity
    private SaleItemEntity toItemEntity(SaleItem d, SaleEntity parent) {
        SaleItemEntity e = SaleItemEntity.builder()
                .sale(parent) // Vinculación Bidireccional
                .quantity(d.quantity())
                .unitPrice(d.unitPrice())
                .systemDiscount(d.systemDiscount())
                .manualDiscount(d.manualDiscount())
                .finalSubtotal(d.finalSubtotal())
                .isPromoLocked(d.isPromoLocked())
                .isPromoForced(d.isPromoForced())
                .promoStrategyApplied(d.promoStrategyApplied())
                .build();

        if (d.productId() != null)
            e.setProduct(entityManager.getReference(ProductEntity.class, d.productId()));
        if (d.decantPriceId() != null)
            e.setDecantPrice(entityManager.getReference(DecantPriceEntity.class, d.decantPriceId()));

        return e;
    }

    // Helper para Payment Entity
    private SalePaymentEntity toPaymentEntity(SalePayment d, SaleEntity parent) {
        SalePaymentEntity e = SalePaymentEntity.builder()
                .sale(parent) // Vinculación Bidireccional
                .amount(d.amount())
                .commissionApplied(d.commission())
                .build();

        if (d.paymentMethodId() != null) {
            e.setPaymentMethod(entityManager.getReference(PaymentMethodEntity.class, d.paymentMethodId()));
        }
        return e;
    }

    // =========================================================================
    // 3. HELPERS DE FORMATO
    // =========================================================================
    private String buildProductName(SaleItemEntity entity) {
        if (entity.getProduct() != null) {
            return entity.getProduct().getBrand() + " " + entity.getProduct().getLine()
                    + " (" + entity.getProduct().getVolumeProductsMl() + "ml)";
        } else if (entity.getDecantPrice() != null) {
            var p = entity.getDecantPrice().getProduct();
            return "DECANT " + p.getBrand() + " " + p.getLine() + " - " + entity.getDecantPrice().getVolumeMl() + "ml";
        }
        return "Desconocido";
    }

    private String extractBrand(SaleItemEntity entity) {
        if (entity.getProduct() != null) return entity.getProduct().getBrand();
        if (entity.getDecantPrice() != null) return entity.getDecantPrice().getProduct().getBrand();
        return "N/A";
    }
}
