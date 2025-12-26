package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reve_back.application.ports.in.GetActivePromotionsUseCase;
import reve_back.application.ports.out.PromotionRepositoryPort;
import reve_back.domain.model.Promotion;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PromotionService implements GetActivePromotionsUseCase {

    private final PromotionRepositoryPort promotionRepositoryPort;

    @Override
    public List<Promotion> execute() {
        // Obtenemos Modelos de Dominio puros
        List<Promotion> activePromotions = promotionRepositoryPort.findAllPromotionsActive();

        // Convertimos a DTOs para la vista (API)
        return activePromotions.stream()
                .map(p -> new Promotion(
                        p.id(),
                        p.name(),
                        p.startDate(),
                        p.endDate(),
                        p.isActive(),
                        p.strategyCode(),
                        p.rules(),
                        p.triggerQuantity()
                ))
                .toList();
    }
}
