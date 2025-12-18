package reve_back.infrastructure.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reve_back.application.ports.in.CreateClientUseCase;
import reve_back.application.ports.in.GetClientPointsUseCase;
import reve_back.application.ports.in.GetLoyaltyStatusUseCase;
import reve_back.application.ports.in.SearchClientUseCase;
import reve_back.infrastructure.web.dto.ClientCreationRequest;
import reve_back.infrastructure.web.dto.ClientPointsResponse;
import reve_back.infrastructure.web.dto.ClientResponse;
import reve_back.infrastructure.web.dto.LoyaltyResponse;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/clients")
public class ClientController {

    private final SearchClientUseCase searchClientUseCase;
    private final CreateClientUseCase createClientUseCase;
    private final GetClientPointsUseCase getClientPointsUseCase;
    private final GetLoyaltyStatusUseCase getLoyaltyStatusUseCase;

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('sales:create:client')")
    public ResponseEntity<List<ClientResponse>> searchClients(@RequestParam("query") String query) {
        return ResponseEntity.ok(searchClientUseCase.searchClients(query));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sales:create:client')")
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody ClientCreationRequest request) {
        ClientResponse response = createClientUseCase.createClient(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{clientId}/point")
    @PreAuthorize("hasAuthority('sales:create:client')")
    public ResponseEntity<ClientPointsResponse> getClientPoints(@PathVariable Long clientId) {
        return ResponseEntity.ok(getClientPointsUseCase.getClientPoints(clientId));
    }

    @GetMapping("/{clientId}/points")
    @PreAuthorize("hasAuthority('sales:create:client')")
    public ResponseEntity<LoyaltyResponse> getStatus(@PathVariable Long clientId) {
        return ResponseEntity.ok(getLoyaltyStatusUseCase.getLoyaltyStatus(clientId));
    }
}
