package reve_back.infrastructure.web.dto;

public record ClientResponse(
        Long id,
        String fullname,
        String dni,
        String email,
        String phone,
        boolean is_vip
) {
}
