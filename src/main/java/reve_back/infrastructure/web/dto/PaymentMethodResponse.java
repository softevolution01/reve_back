package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

public record PaymentMethodResponse(
        Long id,
        String name,
        BigDecimal surcharge_percentage
) {
}
