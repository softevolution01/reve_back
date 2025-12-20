package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.PaymentMethodsRepositoryPort;
import reve_back.domain.model.PaymentMethod;
import reve_back.infrastructure.persistence.jpa.SprigDataPaymentMethodRepository;

import java.util.List;

@RequiredArgsConstructor
@Repository
public class JpaPaymentMethodsRepositoryAdapter implements PaymentMethodsRepositoryPort {

    private final SprigDataPaymentMethodRepository springDataPaymentMethodRepository;

    @Override
    public List<PaymentMethod> findAllActive() {
        // 1. Usamos el método de Spring Data
        var entities = springDataPaymentMethodRepository.findAllByOrderByIdAsc();

        return entities.stream()
                .map(entity -> new PaymentMethod(
                        entity.getId(),
                        entity.getName(),
                        entity.getSurchargePercentage()
                ))
                .toList();
    }
}
