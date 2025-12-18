package reve_back.infrastructure.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reve_back.application.ports.in.CreateSaleUseCase;
import reve_back.infrastructure.web.dto.SaleCreationRequest;
import reve_back.infrastructure.web.dto.SaleResponse;

@RequiredArgsConstructor
@RestController
@RequestMapping("/sales")
public class SaleController {

    private final CreateSaleUseCase createSaleUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('sale:create')")
    public ResponseEntity<SaleResponse> createSale(@Valid @RequestBody SaleCreationRequest request) {
        SaleResponse response = createSaleUseCase.createSale(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
