package reve_back.application.ports.out;

import reve_back.domain.model.ClientLoyaltyProgress;

import java.util.Optional;

public interface LoyaltyProgressRepositoryPort {
    Optional<ClientLoyaltyProgress> findByClientId(Long clientId);
    void save(ClientLoyaltyProgress progress);
}
