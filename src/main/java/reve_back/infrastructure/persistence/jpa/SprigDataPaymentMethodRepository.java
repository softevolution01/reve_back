package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import reve_back.infrastructure.persistence.entity.PaymentMethodEntity;

import java.util.List;

public interface SprigDataPaymentMethodRepository extends JpaRepository<PaymentMethodEntity, Long> {
    List<PaymentMethodEntity> findAllByOrderByIdAsc();
}
