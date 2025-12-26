package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.ClientPointsResponse;

public interface GetClientPointsUseCase {
    ClientPointsResponse getClientPoints(Long clientId);
}
