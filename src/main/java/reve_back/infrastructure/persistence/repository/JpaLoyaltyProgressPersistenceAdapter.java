package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.LoyaltyProgressRepositoryPort;
import reve_back.domain.model.ClientLoyaltyProgress;
import reve_back.infrastructure.persistence.entity.ClientLoyaltyProgressEntity;
import reve_back.infrastructure.persistence.jpa.SpringLoyaltyProgressRepository;
import reve_back.infrastructure.persistence.mapper.PersistenceMapper;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class JpaLoyaltyProgressPersistenceAdapter implements LoyaltyProgressRepositoryPort {

    private final SpringLoyaltyProgressRepository springLoyaltyProgressRepository;
    private final PersistenceMapper mapper;

    @Override
    public Optional<ClientLoyaltyProgress> findByClientId(Long clientId) {
        return springLoyaltyProgressRepository.findById(clientId)
                .map(mapper::toDomain);
    }

    @Override
    public void save(ClientLoyaltyProgress progress) {
        ClientLoyaltyProgressEntity entity = mapper.toEntity(progress);
        springLoyaltyProgressRepository.save(entity);
    }

}
