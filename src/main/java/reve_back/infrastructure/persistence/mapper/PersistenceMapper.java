package reve_back.infrastructure.persistence.mapper;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reve_back.domain.model.*;
import reve_back.infrastructure.persistence.entity.*;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PersistenceMapper {

    public Branch toDomain(BranchEntity entity) {
        return new Branch(
                entity.getId(),
                entity.getName(),
                entity.getLocation()
        );
    }

    public Warehouse toDomain(WarehouseEntity entity) {
        if (entity == null) return null;
        return new Warehouse(
                entity.getId(),
                entity.getName(),
                entity.getLocation(),
                null
        );
    }

    public WarehouseEntity toEntity(Warehouse domain) {
        if (domain == null) return null;
        WarehouseEntity entity = new WarehouseEntity();
        entity.setId(domain.id());
        entity.setName(domain.name());
        entity.setLocation(domain.location());
        return entity;
    }

    public Product toDomain(ProductEntity entity) {
        if (entity == null) return null;
        return new Product(
                entity.getId(),
                entity.getBrand(),
                entity.getPrice(),
                entity.getLine(),
                entity.getConcentration(),
                entity.getVolumeProductsMl(),
                entity.is_active(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ProductEntity toEntity(Product domain) {
        if (domain == null) return null;
        ProductEntity entity = new ProductEntity();
        entity.setId(domain.id());
        entity.setBrand(domain.brand());
        entity.setPrice(domain.price());
        entity.setLine(domain.line());
        entity.setConcentration(domain.concentration());
        entity.setVolumeProductsMl(domain.volumeProductsMl());
        entity.set_active(domain.isActive());
        // createdAt y updatedAt no se setean manualmente, los maneja Hibernate
        return entity;
    }


    public DecantPrice toDomain(DecantPriceEntity entity) {
        if (entity == null) return null;
        return new DecantPrice(entity.getId(), entity.getProductId(), entity.getVolumeMl(),
                entity.getPrice(), entity.getBarcode(), entity.getImageBarcode());
    }

    public DecantPriceEntity toEntity(DecantPrice domain) {
        if (domain == null) return null;
        DecantPriceEntity entity = new DecantPriceEntity();
        entity.setId(domain.id());
        entity.setProductId(domain.productId());
        entity.setVolumeMl(domain.volumeMl());
        entity.setPrice(domain.price());
        entity.setBarcode(domain.barcode());
        entity.setImageBarcode(domain.imageBarcode());
        return entity;
    }

    public InventoryMovement toDomain(InventoryMovementEntity entity) {
        if (entity == null) return null;

        return new InventoryMovement(
                entity.getId(),
                entity.getBottleId(),
                entity.getQuantity(),
                entity.getType().name(), // Convierte el Enum INGRESO/EGRESO a String
                entity.getUnit(),
                entity.getReason(),
                entity.getUserId(),
                entity.getCreatedAt()
        );
    }

    public InventoryMovementEntity toEntity(InventoryMovement domain) {
        if (domain == null) return null;

        InventoryMovementEntity entity = new InventoryMovementEntity();
        entity.setId(domain.id());
        entity.setBottleId(domain.bottleId());
        entity.setQuantity(domain.quantity());

        if (domain.type() != null) {
            entity.setType(MovementType.valueOf(domain.type().toUpperCase()));
        }

        entity.setReason(domain.reason());
        entity.setUserId(domain.userId());

        return entity;
    }

    public ClientLoyaltyProgress toDomain(ClientLoyaltyProgressEntity entity) {
        if (entity == null) return null;
        return new ClientLoyaltyProgress(
                entity.getClientId(),
                entity.getCurrentTier(),
                entity.getPointsInTier(),
                entity.getAccumulatedMoney(),
                entity.getUpdatedAt()
        );
    }

    public ClientLoyaltyProgressEntity toEntity(ClientLoyaltyProgress domain) {
        if (domain == null) return null;
        return ClientLoyaltyProgressEntity.builder()
                .clientId(domain.clientId())
                .currentTier(domain.currentTier())
                .pointsInTier(domain.pointsInTier())
                .accumulatedMoney(domain.accumulatesMoney())
                .updatedAt(domain.updateAt())
                .build();
    }

    public LoyaltyTiers toDomain(LoyaltyTiersEntity entity){
        if (entity == null) return null;
        return new LoyaltyTiers(
                entity.getTierLevel(),
                entity.getCostPerPoint()
        );
    }

    public LoyaltyTiersEntity toEntity(LoyaltyTiers domain){
        if (domain == null) return null;
        return LoyaltyTiersEntity.builder()
                .tierLevel(domain.tierLevel())
                .costPerPoint(domain.costPerPoint())
                .build();
    }

    private final EntityManager entityManager;

    public Bottle toDomain(BottleEntity entity) {
        if (entity == null) return null;
        return new Bottle(
                entity.getId(),
                entity.getProductId(),
                entity.getWarehouse().getId(), // Extraemos solo el ID del objeto Warehouse
                entity.getStatus(),
                entity.getBarcode(),
                entity.getVolumeMl(),
                entity.getRemainingVolumeMl(),
                entity.getQuantity()
        );
    }

    public BottleEntity toEntity(Bottle domain) {
        if (domain == null) return null;

        BottleEntity entity = new BottleEntity();
        entity.setId(domain.id());
        entity.setProductId(domain.productId());

        // RESOLUCIÓN DEL CONFLICTO:
        // No buscamos el warehouse en la DB, solo creamos una referencia con el ID.
        if (domain.warehouseId() != null) {
            WarehouseEntity warehouse = entityManager.getReference(WarehouseEntity.class, domain.warehouseId());
            entity.setWarehouse(warehouse);
        }

        entity.setStatus(domain.status());
        entity.setBarcode(domain.barcode());
        entity.setVolumeMl(domain.volumeMl());
        entity.setRemainingVolumeMl(domain.remainingVolumeMl());
        entity.setQuantity(domain.quantity());

        return entity;
    }

    public Permission toDomain(PermissionEntity entity){
        return new Permission(entity.getId(),entity.getName());
    }

    public Role toDomain(RoleEntity entity){
        return new Role(
                entity.getId(),
                entity.getName(),
                entity.getPermissions().stream()
                        .map(this::toDomain)
                        .collect(Collectors.toSet())
        );
    }

    public User toDomain(UserEntity entity){
        Long cliendId = (entity.getClient() != null) ? entity.getClient().getId() : null;

        Set<Branch> branches = entity.getBranches().stream()
                .map(this::toDomain)
                .collect(Collectors.toSet());

        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getFullname(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getPasswordHash(),
                entity.getRoles().stream()
                        .map(this::toDomain)
                        .collect(Collectors.toSet()),
                branches,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                cliendId
        );
    }

    // --- Mapeo a Entidad ---
    public PermissionEntity toEntity(Permission domain){
        return null;
    }

    public RoleEntity toEntity(Role domain){
        return null;
    }

    public UserEntity toEntity(User domain){
        UserEntity entity = new UserEntity();
        entity.setId(domain.id());
        entity.setUsername(domain.username());
        entity.setFullname(domain.fullname());
        entity.setEmail(domain.email());
        entity.setPhone(domain.phone());
        entity.setPasswordHash(domain.passwordHash());
        return entity;
    }

    public Client toDomain(ClientEntity entity) {
        if (entity == null) return null;
        return new reve_back.domain.model.Client(
                entity.getId(),
                entity.getFullname(),
                entity.getDni(),
                entity.getEmail(),
                entity.getPhone(),
                entity.isVip(),
                entity.getVipSince(),
                entity.getVipPurchaseCounter(),
                entity.getCreatedAt()
        );
    }
}
