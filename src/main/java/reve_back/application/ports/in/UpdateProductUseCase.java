package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.ProductDetailsResponse;
import reve_back.infrastructure.web.dto.ProductUpdateRequest;

public interface UpdateProductUseCase {
    ProductDetailsResponse updateProduct(Long id, ProductUpdateRequest request);
}
