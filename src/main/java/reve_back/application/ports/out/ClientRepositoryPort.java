package reve_back.application.ports.out;

import reve_back.domain.model.Client;

import java.util.List;

public interface ClientRepositoryPort {

    List<Client> searchByFullnameOrDni(String query);
    Client save(Client client);
    boolean existsByDni(String dni);
    boolean existsByEmail(String email);

}
