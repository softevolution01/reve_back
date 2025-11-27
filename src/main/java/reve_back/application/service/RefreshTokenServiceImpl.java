package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reve_back.application.ports.in.AuthResponse;
import reve_back.application.ports.in.RefreshTokenUseCase;
import reve_back.application.ports.out.JwtTokenPort;
import reve_back.application.ports.out.UserRepositoryPort;
import reve_back.domain.model.User;
import reve_back.infrastructure.web.dto.RefreshTokenRequest;

@RequiredArgsConstructor
@Service
public class RefreshTokenServiceImpl implements RefreshTokenUseCase {

    private final JwtTokenPort jwtTokenPort;
    private final UserRepositoryPort userRepositoryPort;


    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtTokenPort.isTokenValid(refreshToken)) {
            throw new RuntimeException("Refresh Token inválido o expirado. Por favor inicie sesión nuevamente.");
        }

        String username = jwtTokenPort.extractUsername(refreshToken);

        User user = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));

        String newAccessToken = jwtTokenPort.generateToken(user);

        return new AuthResponse(newAccessToken, refreshToken);
    }
}
