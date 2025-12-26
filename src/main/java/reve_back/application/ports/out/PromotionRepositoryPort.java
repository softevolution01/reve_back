package reve_back.application.ports.out;

import reve_back.domain.model.Promotion;

import java.util.List;
import java.util.Optional;

public interface PromotionRepositoryPort {
    Optional<Promotion> findActivePromotionById(Long id);
    List<Promotion> findAllPromotionsActive();
}
