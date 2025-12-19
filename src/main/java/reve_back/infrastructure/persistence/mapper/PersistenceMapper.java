package reve_back.infrastructure.persistence.mapper;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reve_back.domain.model.*;
import reve_back.infrastructure.persistence.entity.*;
import reve_back.infrastructure.persistence.enums.global.MovementUnit;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PersistenceMapper {

    public Branch toDomain(BranchEntity entity) {
        if (entity == null) return null;
        return new Branch(
                entity.getId(),
                entity.getName(),
                entity.getLocation(),
                entity.getWarehouse() != null ? entity.getWarehouse().getId() : null,
                entity.getIsCashManagedCentralized()
        );
    }

    public Warehouse toDomain(WarehouseEntity entity) {
        if (entity == null) return null;
        return new Warehouse(
                entity.getId(),
                entity.getName(),
                entity.getLocation()
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
                entity.getLine(),
                entity.getConcentration(),
                entity.getPrice(),
                entity.getVolumeProductsMl(),
                entity.isActive(),
                entity.getAllowPromotions(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ProductEntity toEntity(Product domain) {
        if (domain == null) return null;
        ProductEntity entity = new ProductEntity();
        entity.setId(domain.id());
        entity.setBrand(domain.brand());
        entity.setLine(domain.line());
        entity.setConcentration(domain.concentration());
        entity.setVolumeProductsMl(domain.volumeProductsMl());
        entity.setPrice(domain.price());
        entity.setActive(domain.isActive());
        entity.setAllowPromotions(domain.allowPromotions());
        return entity;
    }


    public DecantPrice toDomain(DecantPriceEntity entity) {
        if (entity == null) return null;
        return new DecantPrice(
                entity.getId(),
                entity.getProduct().getId(),
                entity.getVolumeMl(),
                entity.getPrice(),
                entity.getBarcode(),
                entity.getImageBarcode()
        );
    }

    public DecantPriceEntity toEntity(DecantPrice domain) {
        if (domain == null) return null;
        DecantPriceEntity entity = new DecantPriceEntity();
        entity.setId(domain.id());
        entity.setVolumeMl(domain.volumeMl());
        entity.setPrice(domain.price());
        entity.setBarcode(domain.barcode());

        if (domain.productId() != null) {
            entity.setProduct(entityManager.getReference(ProductEntity.class, domain.productId()));
        }

        return entity;
    }

    public InventoryMovement toDomain(InventoryMovementEntity entity) {
        if (entity == null) return null;
        return new InventoryMovement(
                entity.getId(),
                entity.getBottleId(),
                entity.getQuantity(),
                entity.getType().name(),
                entity.getUnit().name(),
                entity.getReason(),
                entity.getUserId(),
                entity.getCreatedAt()
        );
    }

    public InventoryMovementEntity toEntity(InventoryMovement domain) {
        if (domain == null) return null;
        InventoryMovementEntity entity = new InventoryMovementEntity();
        entity.setId(domain.id());
        entity.setQuantity(domain.quantity());
        entity.setReason(domain.reason());

        if (domain.type() != null) entity.setType(MovementType.valueOf(domain.type()));
        if (domain.unit() != null) entity.setUnit(MovementUnit.valueOf(domain.unit()));

        // Relaciones
        if (domain.bottleId() != null) {
            entity.setBottleId(domain.bottleId());
        }
        if (domain.userId() != null) {
            entity.setUserId(domain.userId());
        }

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
                .accumulatedMoney(domain.accumulatedMoney())
                .build();
    }

    public LoyaltyTiers toDomain(LoyaltyTiersEntity entity){
        if (entity == null) return null;
        return new LoyaltyTiers(
                entity.getTierLevel(),
                entity.getCostPerPoint()
        );
    }

    private final EntityManager entityManager;

    public Bottle toDomain(BottleEntity entity) {
        if (entity == null) return null;
        return new Bottle(
                entity.getId(),
                entity.getProduct().getId(),
                entity.getWarehouse().getId(),
                entity.getStatus().name(), // Enum -> String
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

        // Conversión String -> Enum
        if (domain.status() != null) {
            entity.setStatus(BottlesStatus.valueOf(domain.status().toUpperCase()));
        }

        entity.setBarcode(domain.barcode());
        entity.setVolumeMl(domain.volumeMl());
        entity.setRemainingVolumeMl(domain.remainingVolumeMl());
        entity.setQuantity(domain.quantity());

        // Relaciones
        if (domain.productId() != null) {
            entity.setProduct(entityManager.getReference(ProductEntity.class, domain.productId()));
        }
        if (domain.warehouseId() != null) {
            entity.setWarehouse(entityManager.getReference(WarehouseEntity.class, domain.warehouseId()));
        }

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
        return new Client(
                entity.getId(),
                entity.getFullname(),
                entity.getDni(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getIsVip(),
                entity.getVipSince(),
                entity.getVipPurchaseCounter(),
                entity.getCreatedAt()
        );
    }
}
