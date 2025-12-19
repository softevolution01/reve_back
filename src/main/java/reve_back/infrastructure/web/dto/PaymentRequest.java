package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

public record PaymentRequest(
        Long paymentMethodId,
        BigDecimal amount
) {}
