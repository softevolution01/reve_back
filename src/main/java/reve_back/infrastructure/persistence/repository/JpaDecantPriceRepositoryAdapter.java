package reve_back.infrastructure.persistence.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.out.DecantPriceRepositoryPort;
import reve_back.domain.model.DecantPrice;
import reve_back.infrastructure.persistence.entity.DecantPriceEntity;
import reve_back.infrastructure.persistence.entity.ProductEntity;
import reve_back.infrastructure.persistence.jpa.SpringDataDecantPriceRepository;
import reve_back.infrastructure.persistence.mapper.PersistenceMapper;
import reve_back.infrastructure.util.BarcodeGenerator;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class JpaDecantPriceRepositoryAdapter implements DecantPriceRepositoryPort {

    private final SpringDataDecantPriceRepository repository;
    private final PersistenceMapper mapper;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public List<DecantPrice> saveAllForProduct(Long productId, List<DecantPrice> decants) {
        ProductEntity productRef = entityManager.getReference(ProductEntity.class, productId);

        List<DecantPriceEntity> entities = decants.stream()
                .map(d -> {
                    // Convertimos Domain -> Entity
                    DecantPriceEntity e = mapper.toEntity(d);

                    // 2. Asignamos la relación JPA correctamente
                    // (En lugar de setProductId, usamos setProduct con la referencia)
                    e.setProduct(productRef);

                    // 3. Lógica de Negocio: Generación automática de Barcode/Imagen
                    if (e.getBarcode() == null || e.getBarcode().isBlank()) {
                        String code = BarcodeGenerator.generateAlphanumeric(12);
                        e.setBarcode(code);
                        // Asumiendo que agregaste este campo a tu entidad como vimos en el mapper anterior
                        e.setImageBarcode("/storage/barcodes/" + code + ".png");
                    }

                    return e;
                })
                .collect(Collectors.toList());

        // 4. Guardamos y volvemos a convertir a Domain
        return repository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DecantPrice> findAllByProductId(Long productId) {
        return repository.findByProductId(productId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DecantPrice> findByBarcode(String barcode) {
        return repository.findByBarcode(barcode)
                .map(mapper::toDomain);
    }
}
