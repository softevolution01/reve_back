package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.RefreshTokenRequest;

public interface RefreshTokenUseCase {
    AuthResponse refreshToken(RefreshTokenRequest request);
}
