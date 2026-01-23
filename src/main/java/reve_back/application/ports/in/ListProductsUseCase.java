package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.ProductPageResponse;

public interface ListProductsUseCase {
    ProductPageResponse findAll(int page, int size, String query);
}
