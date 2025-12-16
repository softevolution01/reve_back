package reve_back.infrastructure.persistence.repository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.DecantPriceRepositoryPort;
import reve_back.domain.model.DecantPrice;
import reve_back.infrastructure.persistence.entity.DecantPriceEntity;
import reve_back.infrastructure.persistence.jpa.SpringDataDecantPriceRepository;
import reve_back.infrastructure.persistence.mapper.PersistenceMapper;
import reve_back.infrastructure.util.BarcodeGenerator;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class JpaDecantPriceRepositoryAdapter implements DecantPriceRepositoryPort {

    private final SpringDataDecantPriceRepository repository;
    private final PersistenceMapper mapper;

    @Override
    @Transactional
    public List<DecantPrice> saveAllForProduct(Long productId, List<DecantPrice> decants) {
        List<DecantPriceEntity> entities = decants.stream().map(d -> {
            DecantPriceEntity e = mapper.toEntity(d);
            e.setProductId(productId);
            if (e.getBarcode() == null) {
                String code = BarcodeGenerator.generateAlphanumeric(12);
                e.setBarcode(code);
                e.setImageBarcode("/storage/barcodes/" + code + ".png"); // Lógica de imagen
            }
            return e;
        }).toList();

        return repository.saveAll(entities).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<DecantPrice> findAllByProductId(Long productId) {
        return repository.findByProductId(productId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<DecantPrice> findByBarcode(String barcode) {
        return repository.findByBarcode(barcode)
                .map(mapper::toDomain);
    }
}
