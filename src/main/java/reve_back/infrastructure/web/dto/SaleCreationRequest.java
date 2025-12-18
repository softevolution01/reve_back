package reve_back.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record SaleCreationRequest(
        @JsonProperty("client_id")
        Long clientId, // Puede ser null (Cliente Anónimo)

        @JsonProperty("branch_id")
        @NotNull(message = "La sucursal es obligatoria")
        Long branchId,

        @JsonProperty("total_amount")
        @NotNull(message = "El monto total es obligatorio")
        BigDecimal totalAmount,

        @JsonProperty("promotion_id")
        Long promotionId,

        @NotEmpty(message = "La venta debe tener al menos un producto")
        @JsonProperty("items")
        List<SaleItemRequest> items,

        @NotEmpty(message = "La venta debe tener al menos un método de pago")
        @JsonProperty("pagos")
        List<PaymentRequest> pagos
) {
}
