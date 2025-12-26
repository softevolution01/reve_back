package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.in.GetPaymentMethodsUseCase;
import reve_back.application.ports.out.PaymentMethodsRepositoryPort;
import reve_back.domain.model.PaymentMethod;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PaymentMethodService implements GetPaymentMethodsUseCase {

    private final PaymentMethodsRepositoryPort paymentMethodsRepositoryPort;

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethod> findAll() {
        return paymentMethodsRepositoryPort.findAllActive();
    }
}
