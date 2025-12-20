package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

import java.math.BigDecimal;

public record PaymentRequest(
        Long paymentMethodId,          // ID del método (1, 2, etc.)
        BigDecimal amount,      // Cuánto pagó
        BigDecimal commission   // <--- ESTE ES EL CAMPO QUE FALTA
) {}
