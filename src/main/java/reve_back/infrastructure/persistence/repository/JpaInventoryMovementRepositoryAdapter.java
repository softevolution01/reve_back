package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.InventoryMovementRepositoryPort;
import reve_back.domain.model.InventoryMovement;
import reve_back.infrastructure.persistence.entity.InventoryMovementEntity;
import reve_back.infrastructure.persistence.jpa.SpringDataMovementRepository;
import reve_back.infrastructure.persistence.mapper.PersistenceMapper;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaInventoryMovementRepositoryAdapter implements InventoryMovementRepositoryPort {
    private final SpringDataMovementRepository repository;
    private final PersistenceMapper mapper;

    @Override
    public InventoryMovement save(InventoryMovement movement) {
        InventoryMovementEntity entity = mapper.toEntity(movement);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public List<InventoryMovement> findAllByBottleId(Long bottleId) {
        return repository.findByBottleIdOrderByCreatedAtDesc(bottleId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}