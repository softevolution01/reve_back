package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reve_back.infrastructure.persistence.entity.DecantPriceEntity;

import java.util.List;

@RepositoryRestResource(exported = false)
public interface SpringDataDecantPriceRepository extends JpaRepository<DecantPriceEntity, Long> {
    List<DecantPriceEntity> findByProductId(Long productId);
}
