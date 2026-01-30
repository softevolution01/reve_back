package reve_back.infrastructure.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reve_back.application.service.ContractService;
import reve_back.infrastructure.web.dto.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/contracts")
@RequiredArgsConstructor
public class ContractController {
    private final ContractService contractService;

    @GetMapping("/products-lookup")
    public ResponseEntity<List<ProductContractLookupResponse>> lookupProducts(
            @RequestParam Long branchId,
            @RequestParam String query
    ) {
        return ResponseEntity.ok(contractService.findProductsForContract(branchId, query));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sales:create:client')")
    public ResponseEntity<?> createContract(@Valid @RequestBody ContractCreationRequest request) {
        try {
            contractService.createContract(request);
            return ResponseEntity.ok(Map.of("message", "Contrato registrado con éxito"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<Page<ContractListResponse>> listContracts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(contractService.getAllContracts(page, size));
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<?> finalizeContract(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam Long paymentMethodId
    ) {
        try {
            contractService.finalizeContract(id, userId, paymentMethodId);
            return ResponseEntity.ok(Map.of("message", "Contrato finalizado y saldo ingresado a caja"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
