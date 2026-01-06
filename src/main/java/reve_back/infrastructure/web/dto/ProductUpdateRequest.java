package reve_back.infrastructure.web.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ProductUpdateRequest(
        String brand,
        String line,
        String concentration,
        Double price,
        Integer unitVolumeMl,
        Boolean allowPromotions,
        @NotEmpty(message = "Debes especificar al menos una botella/sucursal")
        List<BottleCreationRequest> bottles,
        List<DecantRequest> decants
) {
}
