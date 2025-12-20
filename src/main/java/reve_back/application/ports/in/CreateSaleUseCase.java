package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.SaleSimulationRequest;

public interface CreateSaleUseCase {
    Long execute(SaleSimulationRequest request);
}
