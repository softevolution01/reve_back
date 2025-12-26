package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.ClientResponse;

import java.util.List;

public interface SearchClientUseCase {
    List<ClientResponse> searchClients(String query);
}
