package reve_back.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ClientCreationRequest(
        @NotBlank(message = "El nombre completo es requerido")
        String fullname,
        String dni,
        String email,
        String phone
) {
}
