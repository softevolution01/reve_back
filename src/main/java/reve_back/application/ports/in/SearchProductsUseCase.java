package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.ProductSearchResponse;

import java.util.List;

public interface SearchProductsUseCase {
    List<ProductSearchResponse> searchProducts(String term);
}
