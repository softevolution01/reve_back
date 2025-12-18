package reve_back.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SaleItemRequest(
        @JsonProperty("tipo_vendible")
        @NotNull(message = "El tipo vendible (Botella/Decant) es obligatorio")
        String tipoVendible,

        @JsonProperty("id_inventario")
        @NotNull(message = "El ID del inventario es obligatorio")
        Long idInventario,

        @Positive(message = "La cantidad debe ser mayor a 0")
        @NotNull
        Integer quantity,

        @Positive(message = "El precio de venta es obligatorio")
        @NotNull
        BigDecimal price,

        BigDecimal discount,

        @JsonProperty("extra_discount")
        BigDecimal extraDiscount
) {
}
