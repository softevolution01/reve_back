package reve_back.infrastructure.persistence.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reve_back.application.ports.out.JwtTokenPort;
import reve_back.domain.model.Branch;
import reve_back.domain.model.Permission;
import reve_back.domain.model.Role;
import reve_back.domain.model.User;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JwtTokenAdapter implements JwtTokenPort {

    @Value("${jwt.secret.key}")
    private String SECRET_KEY;

    @Value("${jwt.expiration.ms}")
    private long JWT_EXPIRATION_MS;

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
        List<String> branchNames = user.branches().stream()
                .map(Branch::name)
                .collect(Collectors.toList());
        claims.put("branches", branchNames);

        claims.put("fullname", user.fullname());
        claims.put("email", user.email());

        return Jwts.builder()
                .claims(claims)
                .subject(user.username())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION_MS))
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
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
