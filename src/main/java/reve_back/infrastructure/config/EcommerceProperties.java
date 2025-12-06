package reve_back.infrastructure.config;

import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Component
public class EcommerceProperties {

    // CORS
    @Value("${api.cors.allowed-origins}")
    private String allowedOrigins;

    // JWT
    @Value("${jwt.secret.key}")
    private String SECRET_KEY;

    @Value("${jwt.expiration.ms}")
    private long JWT_EXPIRATION_MS;

    @Value("${jwt.refresh.expiration.ms}")
    private long JWT_REFRESH_EXPIRATION_MS;
}
