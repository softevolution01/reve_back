package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.BottleRepositoryPort;
import reve_back.domain.model.Bottle;
import reve_back.infrastructure.persistence.entity.BottleEntity;
import reve_back.infrastructure.persistence.entity.WarehouseEntity;
import reve_back.infrastructure.persistence.jpa.SpringDataBottleRepository;
import reve_back.infrastructure.persistence.jpa.SpringDataWarehouseRepository;
import reve_back.infrastructure.util.BarcodeGenerator;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class JpaBottleRepositoryAdapter implements BottleRepositoryPort {

    private final SpringDataBottleRepository springDataBottleRepository;
    private final SpringDataWarehouseRepository springDataWarehouseRepository;

    @Override
    public List<Bottle> saveAll(List<Bottle> bottles) {

        Set<Long> warehouseIds = bottles.stream()
                .map(Bottle::warehouseId)
                .collect(Collectors.toSet());

        Map<Long, WarehouseEntity> warehouseCache = springDataWarehouseRepository.findAllById(warehouseIds).stream()
                .collect(Collectors.toMap(WarehouseEntity::getId, Function.identity()));

        List<BottleEntity> entities = bottles.stream()
                .map(bottle -> {
                    // 3. Obtener la entidad Warehouse del caché
                    WarehouseEntity warehouse = warehouseCache.get(bottle.warehouseId());
                    if (warehouse == null) {
                        throw new IllegalArgumentException("Warehouse no encontrado con ID: " + bottle.warehouseId());
                    }

                    // 4. Mapeo al constructor de BottleEntity
                    //    Asegúrate de que la firma del constructor coincida con el orden de los argumentos:
                    return new BottleEntity(
                            null,
                            bottle.productId(),
                            warehouse,
                            bottle.status(),
                            bottle.barcode(),
                            bottle.volumeMl(),
                            bottle.remainingVolumeMl(),
                            bottle.quantity(),
                            null,
                            null
                    );
                })
                .collect(Collectors.toList());

        List<BottleEntity> savedEntities = springDataBottleRepository.saveAll(entities);
        return savedEntities.stream()
                .map(entity -> new Bottle(
                        entity.getId(),
                        entity.getProductId(),
                        entity.getStatus(),
                        entity.getBarcode(),
                        entity.getVolumeMl(),
                        entity.getRemainingVolumeMl(),
                        entity.getQuantity(),
                        entity.getWarehouse().getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Bottle> findAllByProductId(Long productId) {
        List<BottleEntity> bottleEntities = springDataBottleRepository.findByProductId(productId);
        return bottleEntities.stream()
                .map(entity -> new Bottle(
                        entity.getId(),
                        entity.getProductId(),
                        entity.getStatus(),
                        entity.getBarcode(),
                        entity.getVolumeMl(),
                        entity.getRemainingVolumeMl(),
                        entity.getQuantity(),
                        entity.getWarehouse().getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Bottle> updateAll(List<Bottle> bottles) {
        if (bottles == null || bottles.isEmpty()) {
            return List.of();
        }

        Set<Long> warehouseIds = bottles.stream()
                .map(Bottle::warehouseId)
                .collect(Collectors.toSet());

        Map<Long, WarehouseEntity> warehouseCache = springDataWarehouseRepository.findAllById(warehouseIds).stream()
                .collect(Collectors.toMap(WarehouseEntity::getId, Function.identity()));

        List<BottleEntity> entities = bottles.stream()
                .map(b -> {
                    BottleEntity entity = b.id() != null
                            ? springDataBottleRepository.findById(b.id())
                            .orElseThrow(() -> new RuntimeException("Botella no encontrada: " + b.id()))
                            : new BottleEntity();

                    entity.setProductId(b.productId());
                    entity.setStatus(b.status() != null ? b.status() : "agotada");
                    entity.setBarcode(b.barcode() != null ? b.barcode() : BarcodeGenerator.generateAlphanumeric(12));
                    entity.setVolumeMl(b.volumeMl() != null ? b.volumeMl() : 100);
                    entity.setRemainingVolumeMl(b.remainingVolumeMl() != null ? b.remainingVolumeMl() : 100);

                    entity.setQuantity(b.quantity() != null && b.quantity() > 0 ? b.quantity() : 1);

                    WarehouseEntity warehouse = warehouseCache.get(b.warehouseId());
                    if (warehouse == null) {
                        throw new IllegalArgumentException("Warehouse no encontrado con ID: " + b.warehouseId());
                    }
                    entity.setWarehouse(warehouse);

                    return entity;
                })
                .toList();

        List<BottleEntity> saved = springDataBottleRepository.saveAllAndFlush(entities);

        return saved.stream()
                .map(e -> new Bottle(
                        e.getId(),
                        e.getProductId(),
                        e.getStatus(),
                        e.getBarcode(),
                        e.getVolumeMl(),
                        e.getRemainingVolumeMl(),
                        e.getQuantity(),
                        e.getWarehouse().getId()
                ))
                .toList();
    }

    @Override
    public Optional<Bottle> findByBarcodeAndStatus(String barcode, String status) {
        return springDataBottleRepository.findByBarcodeAndStatus(barcode, status)
                .map(entity -> new Bottle(
                        entity.getId(),
                        entity.getProductId(),
                        entity.getStatus(),
                        entity.getBarcode(),
                        entity.getVolumeMl(),
                        entity.getRemainingVolumeMl(),
                        entity.getQuantity(),
                        entity.getWarehouse().getId()
                ));
    }
}
