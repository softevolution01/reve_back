package reve_back.application.ports.in;

public interface RegisterUseCase {
    AuthResponse register(RegisterCommand command);
}
