package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reve_back.application.ports.in.AuthResponse;
import reve_back.application.ports.in.LoginCommand;
import reve_back.application.ports.in.LoginUseCase;
import reve_back.application.ports.out.JwtTokenPort;
import reve_back.application.ports.out.UserRepositoryPort;
import reve_back.domain.model.User;

@RequiredArgsConstructor
@Service
public class LoginServiceImpl implements LoginUseCase {

    private final AuthenticationManager authenticationManager;
    private final UserRepositoryPort userRepositoryPort;
    private final JwtTokenPort jwtTokenPort;

    @Override
    public AuthResponse login(LoginCommand command) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        command.username(),
                        command.password()
                )
        );
        User user = userRepositoryPort.findByUsername(command.username())
                .orElseThrow(()-> new RuntimeException("Usuario autenticado no encontrado en BBDD."));

        String token = jwtTokenPort.generateToken(user);
        return new AuthResponse(token);
    }
}
