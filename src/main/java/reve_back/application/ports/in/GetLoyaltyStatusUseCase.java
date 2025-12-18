package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.LoyaltyResponse;

public interface GetLoyaltyStatusUseCase {
    LoyaltyResponse getLoyaltyStatus(Long clientId);
}
