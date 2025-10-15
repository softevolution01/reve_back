package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import reve_back.infrastructure.persistence.entity.BottleEntity;

import java.util.List;

public interface SpringDataBottleRepository extends JpaRepository<BottleEntity,Long> {
    List<BottleEntity> findByProductId(Long productId);
}
