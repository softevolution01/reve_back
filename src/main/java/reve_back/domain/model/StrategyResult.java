package reve_back.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public record StrategyResult(
        BigDecimal totalDiscount,      // Suma de los regalos aplicados
        ArrayList<Long> lockedTempItemIds,  // IDs (tempId) de los decants que se bloquearon
        String strategyCode            // Código de la promo aplicada
) {}
