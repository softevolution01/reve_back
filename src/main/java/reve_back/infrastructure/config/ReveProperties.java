package reve_back.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "reve")
public record ReveProperties(
        Cors cors,
        Jwt jwt
) {
    // Mapea: reve.cors
    public record Cors(
            String allowedOrigins
    ) {}
    // Mapea: reve.jwt
    public record Jwt(
            String secretKey,
            long expirationMs,
            long refreshExpirationMs
    ) {}
}
