package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import reve_back.infrastructure.persistence.entity.BottleEntity;

public interface SpringDataBottleRepository extends JpaRepository<BottleEntity,Long> {
}
