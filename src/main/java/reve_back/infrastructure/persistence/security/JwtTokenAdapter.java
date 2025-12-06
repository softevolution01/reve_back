package reve_back.infrastructure.persistence.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reve_back.application.ports.out.JwtTokenPort;
import reve_back.domain.model.Permission;
import reve_back.domain.model.Role;
import reve_back.domain.model.User;
import reve_back.infrastructure.config.EcommerceProperties;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class JwtTokenAdapter implements JwtTokenPort {

    private final EcommerceProperties properties;

    @Override
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();

        List<String> permissions = user.roles().stream()
                .flatMap(role -> role.permissions().stream())
                .map(Permission::name)
                .distinct()
                .collect(Collectors.toList());

        claims.put("permissions", permissions);

        List<String> roleNames = user.roles().stream()
                        .map(Role::name)
                                .collect(Collectors.toList());
        claims.put("roles", roleNames);
        List<Map<String, Object>> branchObjects = user.branches().stream()
                .map(branch -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", branch.id());
                    map.put("name", branch.name());
                    return map;
                })
                .toList();

        claims.put("branches", branchObjects);

        claims.put("fullname", user.fullname());
        claims.put("email", user.email());

        return Jwts.builder()
                .claims(claims)
                .subject(user.username())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + properties.getJWT_EXPIRATION_MS()))
                .signWith(getSigningKey()) // SignatureAlgorithm.HS256
                .compact();
    }

    @Override
    public Claims extractClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public String generateRefreshToken(User user) {
        return generateTokenLogic(user, properties.getJWT_REFRESH_EXPIRATION_MS(), false);
    }

    @Override
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    @Override
    public boolean isTokenValid(String token) {
        try{
            extractClaims(token);
            return !isTokenExpired(token);
        }catch (Exception e){
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(properties.getSECRET_KEY());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String generateTokenLogic(User user, long expiration, boolean includeClaims) {
        Map<String, Object> claims = new HashMap<>();

        if (includeClaims) {
            List<String> permissions = user.roles().stream()
                    .flatMap(role -> role.permissions().stream())
                    .map(Permission::name)
                    .distinct()
                    .collect(Collectors.toList());
            claims.put("permissions", permissions);

            List<String> roleNames = user.roles().stream()
                    .map(Role::name)
                    .collect(Collectors.toList());
            claims.put("roles", roleNames);

            List<Map<String, Object>> branchObjects = user.branches().stream()
                    .map(branch -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", branch.id());
                        map.put("name", branch.name());
                        return map;
                    })
                    .toList();

            claims.put("branches", branchObjects);
            claims.put("fullname", user.fullname());
            claims.put("email", user.email());
        }

        return Jwts.builder()
                .claims(claims)
                .subject(user.username())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }
}
