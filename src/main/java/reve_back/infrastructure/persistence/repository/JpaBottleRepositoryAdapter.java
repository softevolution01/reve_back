package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.BottleRepositoryPort;
import reve_back.domain.model.Bottle;
import reve_back.infrastructure.persistence.entity.BottleEntity;
import reve_back.infrastructure.persistence.jpa.SpringDataBottleRepository;
import reve_back.infrastructure.util.BarcodeGenerator;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class JpaBottleRepositoryAdapter implements BottleRepositoryPort {

    private final SpringDataBottleRepository springDataBottleRepository;

    @Override
    public List<Bottle> saveAll(List<Bottle> bottles) {
        List<BottleEntity> entities = bottles.stream()
                .map(bottle -> new BottleEntity(
                        bottle.id(),
                        bottle.productId(),
                        bottle.warehouseId() != null ? bottle.warehouseId() : 1L,
                        bottle.status(),
                        bottle.barcode(),
                        bottle.volumeMl(),
                        bottle.remainingVolumeMl(),
                        bottle.quantity(),
                        bottle.branchId(),
                        null,
                        null
                ))
                .collect(Collectors.toList());

        List<BottleEntity> savedEntities = springDataBottleRepository.saveAll(entities);
        return savedEntities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Bottle> findAllByProductId(Long productId) {
        return springDataBottleRepository.findByProductId(productId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Bottle> updateAll(List<Bottle> bottles) {
        if (bottles == null || bottles.isEmpty()) {
            return List.of();
        }
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

                    entity.setBranchId(b.branchId());

                    return entity;
                })
                .toList();

        List<BottleEntity> saved = springDataBottleRepository.saveAllAndFlush(entities);

        return springDataBottleRepository.saveAllAndFlush(entities).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Bottle> findByBarcodeAndStatus(String barcode, String status) {
        return springDataBottleRepository.findByBarcodeAndStatus(barcode, status)
                .map(this::toDomain);
    }

    @Override
    public Optional<Bottle> findById(Long id) {
        return springDataBottleRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Bottle save(Bottle bottle) {
        // Reutilizamos la lógica de mapeo que ya tenías en saveAll pero para uno solo
        BottleEntity entity = new BottleEntity(
                bottle.id(),
                bottle.productId(),
                bottle.warehouseId() != null ? bottle.warehouseId() : 1L,
                bottle.status(),
                bottle.barcode(),
                bottle.volumeMl(),
                bottle.remainingVolumeMl(),
                bottle.quantity(),
                bottle.branchId(),
                null, null
        );

        return toDomain(springDataBottleRepository.save(entity));
    }

    private Bottle toDomain(BottleEntity e) {
        return new Bottle(
                e.getId(),
                e.getProductId(),
                e.getStatus(),
                e.getBarcode(),
                e.getVolumeMl(),
                e.getRemainingVolumeMl(),
                e.getQuantity(),
                e.getBranchId(),
                e.getWarehouseId()
        );
    }
}
