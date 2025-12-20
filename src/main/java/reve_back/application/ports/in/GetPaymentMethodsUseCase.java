package reve_back.application.ports.in;

import reve_back.domain.model.PaymentMethod;

import java.util.List;

public interface GetPaymentMethodsUseCase {
    List<PaymentMethod> findAll();
}
