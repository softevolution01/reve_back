package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.in.SearchClientUseCase;
import reve_back.application.ports.out.ClientRepositoryPort;
import reve_back.domain.model.Client;
import reve_back.infrastructure.mapper.ClientDtoMapper;
import reve_back.infrastructure.web.dto.ClientResponse;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ClientService implements SearchClientUseCase {

    private final ClientRepositoryPort clientRepositoryPort;
    private final ClientDtoMapper clientDtoMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> searchClients(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        List<Client> clients = clientRepositoryPort.searchByFullnameOrDni(query.trim());

        return clients.stream()
                .map(clientDtoMapper::toResponse)
                .toList();
    }
}
