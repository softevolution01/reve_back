package reve_back.application.ports.in;

public record RegisterCommand(
        String username,
        String fullname,
        String email,
        String phone,
        String rawPassword
) {
}
