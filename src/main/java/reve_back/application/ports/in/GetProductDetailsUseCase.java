package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.ProductDetailsResponse;

public interface GetProductDetailsUseCase {
    ProductDetailsResponse getProductDetails(Long id);
}
