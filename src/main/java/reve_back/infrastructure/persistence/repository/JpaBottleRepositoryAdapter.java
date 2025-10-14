package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.BottleRepositoryPort;
import reve_back.domain.model.Bottle;
import reve_back.infrastructure.persistence.entity.BottleEntity;
import reve_back.infrastructure.persistence.jpa.SpringDataBottleRepository;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class JpaBottleRepositoryAdapter implements BottleRepositoryPort {

    private final SpringDataBottleRepository springDataBottleRepository;

    @Override
    public List<Bottle> saveAll(List<Bottle> bottles) {
        List<BottleEntity> entities = bottles.stream()
                .map(bottle -> new BottleEntity(
                        null,
                        bottle.productId(),
                        bottle.status(),
                        bottle.barcode(),
                        bottle.volumeMl(),
                        bottle.remainingVolumeMl(),
                        bottle.branchId(),
                        null,
                        null
                ))
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
                        entity.getBranchId()))
                .collect(Collectors.toList());
    }
}
