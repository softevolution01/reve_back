package reve_back.infrastructure.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reve_back.application.ports.in.CreateSaleUseCase;
import reve_back.infrastructure.web.dto.SaleCreationRequest;
import reve_back.infrastructure.web.dto.SaleResponse;

import java.util.Map;

@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SaleController {

    private final CreateSaleUseCase createSaleUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('sales:create:client')")
    public ResponseEntity<?> createSale(@Valid @RequestBody SaleCreationRequest request) {
        try {
            SaleResponse response = createSaleUseCase.createSale(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Error en la venta", "message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno", "message", ex.getMessage()));
        }
    }
}