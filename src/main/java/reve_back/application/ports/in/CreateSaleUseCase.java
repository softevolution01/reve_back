package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.SaleCreationRequest;
import reve_back.infrastructure.web.dto.SaleResponse;

public interface CreateSaleUseCase {
    SaleResponse createSale(SaleCreationRequest request);
}
