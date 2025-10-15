package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.ProductCreationRequest;
import reve_back.infrastructure.web.dto.ProductCreationResponse;

public interface CreateProductUseCase {
    ProductCreationResponse createProduct(ProductCreationRequest request);
}
