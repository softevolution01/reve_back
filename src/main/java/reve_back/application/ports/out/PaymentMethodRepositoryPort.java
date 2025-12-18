package reve_back.application.ports.out;

import reve_back.infrastructure.persistence.entity.PaymentMethodEntity;

import java.util.Optional;

public interface PaymentMethodRepositoryPort {
    Optional<PaymentMethodEntity> findById(Long id);
    Optional<PaymentMethodEntity> findByName(String name);
}
