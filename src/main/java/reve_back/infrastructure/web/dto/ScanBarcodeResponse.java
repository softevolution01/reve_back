package reve_back.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ScanBarcodeResponse(
        @JsonProperty("id_inventario")
        Long idInventario,

        @JsonProperty("tipo_vendible")
        String tipoVendible,

        @JsonProperty("producto_id")
        Long productoId,

        @JsonProperty("nombre_producto")
        String nombreProducto,

        @JsonProperty("line")
        String line,

        @JsonProperty("volume_ml")
        Integer volumeMl,

        @JsonProperty("price")
        Double price
) {
}
