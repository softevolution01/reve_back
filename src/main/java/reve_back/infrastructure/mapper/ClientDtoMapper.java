package reve_back.infrastructure.mapper;

import org.springframework.stereotype.Component;
import reve_back.domain.model.Client;
import reve_back.infrastructure.web.dto.ClientResponse;

@Component
public class ClientDtoMapper {

    public ClientResponse toResponse(Client client){
        return new ClientResponse(
                client.id(),
                client.fullname(),
                client.dni(),
                client.email(),
                client.phone(),
                client.isVip()
        );
    }
}
