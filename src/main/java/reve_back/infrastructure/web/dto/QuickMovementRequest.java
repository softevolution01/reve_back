package reve_back.infrastructure.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record QuickMovementRequest(
        @NotNull(message = "El ID de la botella es obligatorio")
        Long bottleId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer quantity,

        @NotBlank(message = "El tipo de movimiento es obligatorio")
        @Pattern(regexp = "^(INGRESO|EGRESO)$", message = "El tipo debe ser INGRESO o EGRESO")
        String type,

        @NotBlank String unit,

        @NotBlank(message = "El motivo es obligatorio")
        String reason,

        @NotNull(message = "El ID de usuario es obligatorio para la auditoría")
        Long userId
) {}