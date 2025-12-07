package reve_back.infrastructure.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reve_back.application.ports.in.SearchClientUseCase;
import reve_back.infrastructure.web.dto.ClientResponse;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final SearchClientUseCase searchClientUseCase;

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('catalog:read:all')")
    public ResponseEntity<List<ClientResponse>> searchClients(@RequestParam("query") String query) {
        return ResponseEntity.ok(searchClientUseCase.searchClients(query));
    }
}
