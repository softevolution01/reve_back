package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.DecantPriceRepositoryPort;
import reve_back.domain.model.DecantPrice;
import reve_back.infrastructure.persistence.entity.DecantPriceEntity;
import reve_back.infrastructure.persistence.jpa.SpringDataDecantPriceRepository;
import reve_back.infrastructure.util.BarcodeGenerator;
import reve_back.infrastructure.web.dto.DecantRequest;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@RequiredArgsConstructor
@Repository
public class JpaDecantPriceRepositoryAdapter implements DecantPriceRepositoryPort {

    private final SpringDataDecantPriceRepository repository;

    @Override
    public List<DecantPrice> saveAllForProduct(Long productId, List<DecantRequest> decants) {
        if (decants == null || decants.isEmpty()) {
            return List.of();
        }

        List<DecantPriceEntity> entities = decants.stream()
                .map(d -> {
                    DecantPriceEntity e = new DecantPriceEntity();
                    e.setProductId(productId);
                    e.setVolumeMl(d.volumeMl());
                    e.setPrice(d.price());
                    e.setBarcode(BarcodeGenerator.generateAlphanumeric(12));
                    return e;
                })
                .toList();

        List<DecantPriceEntity> saved = repository.saveAll(entities);

        return saved.stream()
                .map(e -> new DecantPrice(e.getId(), e.getProductId(), e.getVolumeMl(), e.getPrice(), e.getBarcode()))
                .toList();
    }

    @Override
    public List<DecantPriceEntity> findAllByProductId(Long productId) {
        return repository.findByProductId(productId);
    }

    @Override
    public Optional<DecantPrice> findByBarcode(String barcode) {
        return repository.findByBarcode(barcode)
                .map(e -> new DecantPrice(
                        e.getId(),
                        e.getProductId(),
                        e.getVolumeMl(),
                        e.getPrice(),
                        e.getBarcode()
                ));
    }
}
