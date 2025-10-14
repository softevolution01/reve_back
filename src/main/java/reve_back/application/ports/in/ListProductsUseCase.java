package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.ProductSummaryDTO;

import java.util.List;

public interface ListProductsUseCase {
    List<ProductSummaryDTO> findAll(int page, int size);
}
