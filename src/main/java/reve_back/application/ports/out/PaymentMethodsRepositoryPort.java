package reve_back.application.ports.out;

import reve_back.domain.model.PaymentMethod;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodsRepositoryPort {
    List<PaymentMethod> findAllActive();
    Optional<PaymentMethod> findById(Long id);
}
