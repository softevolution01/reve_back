package reve_back.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RegisterRequest(
        @NotBlank(message = "El nombre de usuario es requerido")
        @Size(min = 3, max = 20, message = "El nombre de usuario debe tener entre 3 y 20 caracteres")
        String username,

        @NotBlank(message = "El nombre completo es requerido")
        String fullname,

        @NotBlank(message = "El email es requerido")
        @Size(max = 50, message = "El email no debe exceder los 50 caracteres")
        @Email(message = "El formato del email no es válido")
        String email,

        String phone,

        @NotBlank(message = "La contraseña es requerida")
        @Size(min = 6, max = 40, message = "La contraseña debe tener entre 6 y 40 caracteres")
        String password,

        @NotBlank(message = "El nombre del rol es requerido")
        String roleName,

        @NotEmpty(message = "La lista de sedes (branchNames) no puede estar vacía")
        Set<String> branchNames

) {
}
