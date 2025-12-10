package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.ClientRepositoryPort;
import reve_back.domain.model.Client;
import reve_back.infrastructure.persistence.entity.ClientEntity;
import reve_back.infrastructure.persistence.jpa.ClientJpaRepository;
import reve_back.infrastructure.persistence.mapper.PersistenceMapper;

import java.util.List;
import java.util.Optional;

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
    public Client save(Client client) {
        ClientEntity entity = new ClientEntity();

        entity.setFullname(client.fullname());
        entity.setDni(client.dni());
        entity.setEmail(client.email());
        entity.setPhone(client.phone());

        ClientEntity savedEntity = clientJpaRepository.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Client> findById(Long id) {
        return clientJpaRepository.findById(id)
                .map(mapper::toDomain);
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
