package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reve_back.application.ports.in.ListProductsUseCase;
import reve_back.application.ports.out.ProductRepositoryPort;
import reve_back.infrastructure.web.dto.ProductSummaryDTO;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ProductService implements ListProductsUseCase {

    private final ProductRepositoryPort productRepositoryPort;

    @Override
    public List<ProductSummaryDTO> findAll(int page, int size) {
        return productRepositoryPort.findAll(page, size);
    }
}
