package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reve_back.infrastructure.persistence.entity.SalePaymentEntity;

@RepositoryRestResource(exported = false)
public interface SalePaymentJpaRepository extends JpaRepository<SalePaymentEntity, Long> {
}
