package reve_back.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ScanBarcodeResponse(
        Long idInventario,

        String tipoVendible,

        Long productoId,

        String nombreProducto,

        String line,

        String concentration,

        Integer volumeMl,

        Double price,

        Boolean allowPromotions
) {
}
