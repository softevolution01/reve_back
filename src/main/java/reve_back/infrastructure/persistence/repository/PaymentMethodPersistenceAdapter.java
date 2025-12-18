package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.PaymentMethodRepositoryPort;
import reve_back.infrastructure.persistence.entity.PaymentMethodEntity;
import reve_back.infrastructure.persistence.jpa.PaymentMethodJpaRepository;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class PaymentMethodPersistenceAdapter implements PaymentMethodRepositoryPort {

    private final PaymentMethodJpaRepository repository;

    @Override
    public Optional<PaymentMethodEntity> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<PaymentMethodEntity> findByName(String name) {
        return repository.findByName(name);
    }
}
