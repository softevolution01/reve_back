package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.SaleCreationRequest;
import reve_back.infrastructure.web.dto.SaleResponse;
import reve_back.infrastructure.web.dto.SaleSimulationRequest;

public interface CreateSaleUseCase {
    Long execute(SaleSimulationRequest request);
    SaleResponse createSale(SaleCreationRequest request);
}
