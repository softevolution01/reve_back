package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.DecantPriceRepositoryPort;
import reve_back.domain.model.DecantPrice;
import reve_back.infrastructure.persistence.entity.DecantPriceEntity;
import reve_back.infrastructure.persistence.jpa.SpringDataDecantPriceRepository;
import reve_back.infrastructure.web.dto.DecantRequest;

import java.security.SecureRandom;
import java.util.List;
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
                    e.setBarcode(generateBarcode(12));
                    return e;
                })
                .toList();

        List<DecantPriceEntity> saved = repository.saveAll(entities);

        return saved.stream()
                .map(e -> new DecantPrice(e.getId(), e.getVolumeMl(), e.getPrice(), e.getBarcode()))
                .toList();
    }
    private String generateBarcode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
