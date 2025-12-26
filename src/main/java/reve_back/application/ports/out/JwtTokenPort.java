package reve_back.application.ports.out;

import io.jsonwebtoken.Claims;
import reve_back.domain.model.User;

public interface JwtTokenPort {

    String generateToken(User user);
    Claims extractClaims(String token);
    String generateRefreshToken(User user);
    String extractUsername(String token);
    boolean isTokenValid(String token);
}
