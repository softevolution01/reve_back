package reve_back.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record BottleCreationRequest(
//        String barcode,
        @NotNull(message = "La sucursal (branchId) es obligatoria")
        Long branchId,
        @PositiveOrZero(message = "volumeMl debe ser mayor o igual a 0")
        Integer volumeMl,
        @PositiveOrZero(message = "remainingVolumeMl debe ser mayor o igual a 0")
        Integer remainingVolumeMl,
        Integer quantity,
        String status
) {
}
