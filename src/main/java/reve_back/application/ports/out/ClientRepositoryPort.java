package reve_back.application.ports.out;

import reve_back.domain.model.Client;

import java.util.List;
import java.util.Optional;

public interface ClientRepositoryPort {

    List<Client> searchByFullnameOrDni(String query);
    Client save(Client client);
    Optional<Client> findById(Long id);
    boolean existsByDni(String dni);
    boolean existsByEmail(String email);

}
