package reve_back.infrastructure.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reve_back.application.ports.in.InventoryMovementUseCase;
import reve_back.domain.model.InventoryMovement;
import reve_back.infrastructure.web.dto.QuickMovementRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryMovementUseCase inventoryUseCase;

    @PostMapping("/movement")
    @PreAuthorize("hasAuthority('inventory:edit:quantity')")
    public ResponseEntity<?> createMovement(@Valid @RequestBody QuickMovementRequest request) {
        try {
            inventoryUseCase.processMovement(request);
            return ResponseEntity.ok(Map.of("message", "Movimiento procesado y registrado en Kardex"));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));
        }
    }
}