package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reve_back.domain.model.Promotion;
import reve_back.infrastructure.persistence.entity.PromotionEntity;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(exported = false)
public interface SpringDataPromotionRepository extends JpaRepository<PromotionEntity,Long> {
    @Query("SELECT p FROM PromotionEntity p " +
            "LEFT JOIN FETCH p.rules " +
            "WHERE p.id = :id AND p.isActive = true")
    Optional<PromotionEntity> findActivePromotionWithRules(@Param("id") Long id);

    @Query("SELECT p FROM PromotionEntity p WHERE p.isActive = true AND CURRENT_DATE BETWEEN p.startDate AND p.endDate")
    List<PromotionEntity> findAllPromotionsActive();
}
