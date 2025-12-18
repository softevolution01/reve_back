package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.CashMovementRepositoryPort;
import reve_back.infrastructure.persistence.entity.CashMovementEntity;
import reve_back.infrastructure.persistence.jpa.CashMovementJpaRepository;

@RequiredArgsConstructor
@Repository
public class CashMovementPersistenceAdapter implements CashMovementRepositoryPort {

    private final CashMovementJpaRepository repository;

    @Override
    public CashMovementEntity save(CashMovementEntity movement) {
        return repository.save(movement);
    }
}
