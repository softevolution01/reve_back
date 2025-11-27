package reve_back.application.ports.in;

public record AuthResponse(
        String token,
        String refreshToken
) {
}
