package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reve_back.application.ports.in.AuthResponse;
import reve_back.application.ports.in.RegisterCommand;
import reve_back.application.ports.in.RegisterUseCase;
import reve_back.application.ports.out.JwtTokenPort;
import reve_back.application.ports.out.RoleRepositoryPort;
import reve_back.application.ports.out.UserRepositoryPort;
import reve_back.domain.model.Role;
import reve_back.domain.model.User;

import java.util.Set;

@RequiredArgsConstructor
@Service
public class RegisterServiceImpl implements RegisterUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final RoleRepositoryPort roleRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenPort jwtTokenPort;

    @Override
    public AuthResponse register(RegisterCommand command) {
        if (userRepositoryPort.existsByUsername(command.username())){
            throw new RuntimeException("Error: El nombre de usuario ya está en uso.");
        }
        if (userRepositoryPort.existsByEmail(command.username())){
            throw new RuntimeException("Error: El email ya esta en uso.");
        }
        Role roleToAssign = roleRepositoryPort.findByName(command.rolName())
                .orElseThrow(() -> new RuntimeException(
                        "Error: El tipo de rol '" + command.rolName() + "' no existe."));
        String hashedPassword = passwordEncoder.encode(command.rawPassword());

        User newUser = new User(
                command.username(),
                command.fullname(),
                command.email(),
                command.phone(),
                hashedPassword,
                Set.of(roleToAssign),
                null // asigna el rol "Cliente"
        );

        User savedUser = userRepositoryPort.save(newUser);
        String token = jwtTokenPort.generateToken(savedUser);
        return new AuthResponse(token);
    }
}
