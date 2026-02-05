package reve_back.infrastructure.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reve_back.application.ports.in.*;
import reve_back.infrastructure.web.dto.LoginRequest;
import reve_back.infrastructure.web.dto.RefreshTokenRequest;
import reve_back.infrastructure.web.dto.RegisterRequest;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest dto){
        RegisterCommand command = new RegisterCommand(
                dto.username(),
                dto.fullname(),
                dto.email(),
                dto.phone(),
                dto.password(),
                dto.roleName(),
                dto.branchNames()
        );
        return ResponseEntity.ok(registerUseCase.register(command));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest dto){
        LoginCommand command = new LoginCommand(dto.username(), dto.password());
        return ResponseEntity.ok(loginUseCase.login(command));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest dto) {
        return ResponseEntity.ok(refreshTokenUseCase.refreshToken(dto));
    }
}
