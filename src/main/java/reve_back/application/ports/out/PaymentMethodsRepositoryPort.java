package reve_back.application.ports.out;

import reve_back.domain.model.PaymentMethod;

import java.util.List;

public interface PaymentMethodsRepositoryPort {
    List<PaymentMethod> findAllActive();
}
