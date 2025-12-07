package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.ClientRepositoryPort;
import reve_back.domain.model.Client;
import reve_back.infrastructure.persistence.jpa.ClientJpaRepository;
import reve_back.infrastructure.persistence.mapper.PersistenceMapper;

import java.util.List;

@RequiredArgsConstructor
@Repository
public class ClientPersistenceAdapter implements ClientRepositoryPort {

    private final ClientJpaRepository clientJpaRepository;
    private final PersistenceMapper mapper;

    @Override
    public List<Client> searchByFullnameOrDni(String query) {
        return clientJpaRepository.searchByFullnameOrDni(query).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByDni(String dni) {
        return clientJpaRepository.existsByDni(dni);
    }

    @Override
    public boolean existsByEmail(String email) {
        return clientJpaRepository.existsByEmail(email);
    }
}
