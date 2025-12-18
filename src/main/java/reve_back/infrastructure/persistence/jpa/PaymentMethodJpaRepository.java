package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reve_back.infrastructure.persistence.entity.PaymentMethodEntity;

import java.util.Optional;

@RepositoryRestResource(exported = false)
public interface PaymentMethodJpaRepository extends JpaRepository<PaymentMethodEntity, Long> {
    Optional<PaymentMethodEntity> findByName(String name);
}
