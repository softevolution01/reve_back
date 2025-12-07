package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.ClientCreationRequest;
import reve_back.infrastructure.web.dto.ClientResponse;

public interface CreateClientUseCase {
    ClientResponse createClient(ClientCreationRequest request);
}
