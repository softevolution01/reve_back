package reve_back.application.ports.in;

public record LoginCommand(
        String username,
        String password
) {
}
