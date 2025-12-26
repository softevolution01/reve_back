package reve_back.domain.model;
import java.math.BigDecimal;

public record PaymentMethod(
        Long id,
        String name,
        BigDecimal surchargePercentage
) {}
