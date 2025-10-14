package reve_back.application.ports.out;

import reve_back.infrastructure.web.dto.ProductSummaryDTO;

import java.util.List;

public interface ProductRepositoryPort {
    List<ProductSummaryDTO> findAll(int page, int size);
}
