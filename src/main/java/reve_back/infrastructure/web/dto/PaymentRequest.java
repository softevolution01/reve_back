package reve_back.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentRequest(
        @JsonProperty("payment_method_id")
        @NotNull(message = "El método de pago es obligatorio")
        Long paymentMethodId,

        @Positive(message = "El monto debe ser mayor a 0")
        @NotNull
        BigDecimal amount,

        @JsonProperty("commission_applied")
        BigDecimal commissionApplied
) {
}
