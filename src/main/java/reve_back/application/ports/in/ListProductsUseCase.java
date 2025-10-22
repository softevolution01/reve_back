package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.ProductPageResponse;
import reve_back.infrastructure.web.dto.ProductSummaryDTO;

import java.util.List;

public interface ListProductsUseCase {
    ProductPageResponse findAll(int page, int size);
}
