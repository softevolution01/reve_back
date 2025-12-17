package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import reve_back.infrastructure.persistence.entity.InventoryMovementEntity;

import java.util.List;

public interface SpringDataMovementRepository extends JpaRepository<InventoryMovementEntity, Long> {
    List<InventoryMovementEntity> findByBottleIdOrderByCreatedAtDesc(Long bottleId);

    List<InventoryMovementEntity> findByType(String type);

    List<InventoryMovementEntity> findByUserId(Long userId);
}
