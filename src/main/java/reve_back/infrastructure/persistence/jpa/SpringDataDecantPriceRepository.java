package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import reve_back.infrastructure.persistence.entity.DecantPriceEntity;

import java.util.List;

public interface SpringDataDecantPriceRepository extends JpaRepository<DecantPriceEntity, Long> {
    List<DecantPriceEntity> findByProductId(Long productId);
}
