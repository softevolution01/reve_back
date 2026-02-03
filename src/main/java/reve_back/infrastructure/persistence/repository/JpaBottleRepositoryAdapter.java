package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.out.BottleRepositoryPort;
import reve_back.domain.model.Bottle;
import reve_back.domain.model.BottlesStatus;
import reve_back.infrastructure.persistence.entity.BottleEntity;
import reve_back.infrastructure.persistence.jpa.SpringDataBottleRepository;
import reve_back.infrastructure.persistence.jpa.SpringDataWarehouseRepository;
import reve_back.infrastructure.persistence.mapper.PersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class JpaBottleRepositoryAdapter implements BottleRepositoryPort {

    private final SpringDataBottleRepository springDataBottleRepository;
    private final SpringDataWarehouseRepository springDataWarehouseRepository;
    private final PersistenceMapper mapper;

    @Override
    public List<Bottle> saveAll(List<Bottle> bottles) {

        List<BottleEntity> entities = bottles.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());

        List<BottleEntity> savedEntities = springDataBottleRepository.saveAll(entities);
        return savedEntities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bottle> findAllByProductId(Long productId) {
        return springDataBottleRepository.findByProductId(productId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<Bottle> updateAll(List<Bottle> bottles) {
        if (bottles == null || bottles.isEmpty()) {
            return List.of();
        }
        List<BottleEntity> entities = bottles.stream()
                .map(bottle -> {
                    BottleEntity entity = mapper.toEntity(bottle);

                    // Lógica de seguridad para evitar nulos críticos en actualización
                    if (entity.getQuantity() == null) entity.setQuantity(0);
                    if (entity.getRemainingVolumeMl() == null) entity.setRemainingVolumeMl(0);

                    return entity;
                })
                .collect(Collectors.toList());

        List<BottleEntity> saved = springDataBottleRepository.saveAll(entities);

        return saved.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Bottle> findByBarcodeAndStatus(String barcode, BottlesStatus status) {
        try {
            return springDataBottleRepository.findByBarcodeAndStatus(barcode, status)
                    .map(mapper::toDomain);
        } catch (IllegalArgumentException e) {
            // Si pasan un status que no existe (ej: "ROTA"), retornamos vacío
            return Optional.empty();
        }
    }

    // Método auxiliar si necesitas buscar por ID
    @Override
    public Optional<Bottle> findById(Long id) {
        return springDataBottleRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Bottle save(Bottle bottle) {
        BottleEntity entity = mapper.toEntity(bottle);
        BottleEntity saved = springDataBottleRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Bottle> searchActiveByProductName(String term) {
        var pageable = PageRequest.of(0, 5);
        var entities = springDataBottleRepository.findActiveByProductNameLike(term,pageable);
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public Integer calculateTotalStockByProductId(Long productId) {
        // Llamamos a la query nativa/JPQL que acabamos de crear
        return springDataBottleRepository.sumTotalStockByProduct(productId);
    }

    @Override
    public Optional<Bottle> findSellableBottle(Long productId, Long warehouseId) {
        return springDataBottleRepository
                .findSellableBottleForSale(productId, warehouseId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findLastBarcodeByPrefix(String prefix) {
        return springDataBottleRepository.findTopByBarcodeStartsWithOrderByBarcodeDesc(prefix)
                .map(BottleEntity::getBarcode);
    }


}
