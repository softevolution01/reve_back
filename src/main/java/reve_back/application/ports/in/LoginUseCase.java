package reve_back.application.ports.in;

public interface LoginUseCase {
    AuthResponse login(LoginCommand command);
}
