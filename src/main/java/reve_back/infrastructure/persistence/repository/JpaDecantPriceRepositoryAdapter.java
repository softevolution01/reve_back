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
import org.springframework.data.domain.PageRequest;

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
                    DecantPriceEntity e = mapper.toEntity(d);
                    e.setProduct(productRef);
                    if (d.barcode() != null && !d.barcode().isEmpty()) {
                        e.setBarcode(d.barcode());
                    }else {
                        String fallback = BarcodeGenerator.generateNextSequence(null, "D");
                        e.setBarcode(fallback);
                    }
                    return e;
                })
                .collect(Collectors.toList());

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

    @Override
    public List<DecantPrice> searchActiveByProductName(String term) {
        var pageable = PageRequest.of(0, 5);

        var entities = repository.findActiveByProductNameLike(term, pageable);

        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DecantPrice> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findLastBarcodeByPrefix(String prefix) {
        List<String> results = repository.findLastBarcode(prefix, PageRequest.of(0, 1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
