package reve_back.infrastructure.persistence.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import reve_back.application.ports.out.JwtTokenPort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenPort jwtTokenPort;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. SOLUCIÓN CORS: Dejar pasar las peticiones OPTIONS (Preflight) inmediatamente
        // sin buscar headers ni hacer lógica de tokens.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        // 2. Si no hay token, dejamos pasar (útil para /auth/login que es público)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        // 3. Validamos el token
        // Nota: Agregué un try-catch por seguridad, ya que si extractClaims falla
        // podría romper el flujo y causar un error 500 que el navegador interpreta como error de CORS.
        try {
            if (jwtTokenPort.isTokenValid(jwt)) {
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    Claims claims = jwtTokenPort.extractClaims(jwt);
                    String username = claims.getSubject();

                    // Asegúrate que "permissions" existe en el token para evitar NullPointerException
                    List<String> permissionsList = claims.get("permissions", List.class);

                    if (permissionsList == null) {
                        permissionsList = new ArrayList<>(); // Lista vacía si es null
                    }

                    List<GrantedAuthority> authorities = permissionsList.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            authorities);

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Si el token está corrupto o expirado, no detenemos el request aquí,
            // dejamos que SecurityFilterChain decida rechazarlo más adelante.
            // Esto evita errores 500 inesperados.
            logger.error("Error procesando JWT en el filtro: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /*@Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        final String jwt = authHeader.substring(7);
        if (jwtTokenPort.isTokenValid(jwt)) {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                Claims claims = jwtTokenPort.extractClaims(jwt);
                String username = claims.getSubject();

                List<String> permissionsList = (List<String>) claims.get("permissions");

                List<GrantedAuthority> authorities = permissionsList.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username,// El 'principal' es el username
                        null,
                        authorities);

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }*/
}
