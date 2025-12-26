package reve_back.application.service;

import org.springframework.stereotype.Component;
import reve_back.domain.model.PromotionStrategy;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PromotionStrategyFactory {

    private final Map<String, PromotionStrategy> strategies;

    // Inyecta todas las implementaciones de PromotionStrategy (como SandwichNxMStrategy)
    public PromotionStrategyFactory(List<PromotionStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        PromotionStrategy::getStrategyCode,
                        strategy -> strategy
                ));
    }

    public PromotionStrategy getStrategy(String strategyCode) {
        PromotionStrategy strategy = strategies.get(strategyCode);
        if (strategy == null) {
            throw new IllegalArgumentException("Código de estrategia no reconocido: " + strategyCode);
        }
        return strategy;
    }
}