package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.PromotionRepositoryPort;
import reve_back.domain.model.Promotion;
import reve_back.infrastructure.mapper.PromotionDtoMapper;
import reve_back.infrastructure.persistence.entity.PromotionEntity;
import reve_back.infrastructure.persistence.entity.PromotionRuleEntity;
import reve_back.infrastructure.persistence.enums.global.PromotionRuleType;
import reve_back.infrastructure.persistence.jpa.SpringDataPromotionRepository;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class JpaPromotionRepositoryAdapter implements PromotionRepositoryPort {

    private final SpringDataPromotionRepository springDataPromotionRepository; // Usa Entities
    private final PromotionDtoMapper mapper; // Traduce Entity -> Record

    @Override
    public Optional<Promotion> findActivePromotionById(Long id) {
        return springDataPromotionRepository.findActivePromotionWithRules(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Promotion> findAllPromotionsActive() {
        // 1. Obtenemos Entities de la BD
        return springDataPromotionRepository.findAllPromotionsActive().stream()
                // 2. Convertimos a Dominio usando el Mapper (aquí ocurre el cálculo)
                .map(mapper::toDomain)
                .toList();
    }


}
