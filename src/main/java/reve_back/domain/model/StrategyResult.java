package reve_back.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public record StrategyResult(
        BigDecimal totalDiscount,      // Suma de los regalos o descuentos aplicados
        List<Long> lockedTempItemIds,  // Usar List<Long> en lugar de ArrayList<Long>
        String strategyCode            // Código de la promo aplicada
) {}
