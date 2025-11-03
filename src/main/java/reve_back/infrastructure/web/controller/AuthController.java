package reve_back.infrastructure.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reve_back.application.ports.in.*;
import reve_back.infrastructure.web.dto.LoginRequest;
import reve_back.infrastructure.web.dto.RegisterRequest;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest dto){
        RegisterCommand command = new RegisterCommand(
                dto.username(),
                dto.fullname(),
                dto.email(),
                dto.phone(),
                dto.password()
        );
        return ResponseEntity.ok(registerUseCase.register(command));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest dto){
        LoginCommand command = new LoginCommand(dto.username(), dto.password());
        return ResponseEntity.ok(loginUseCase.login(command));
    }
}
