package reve_back.infrastructure.web.dto;

import java.util.List;

public record ProductPageResponse(
        List<ProductSummaryDTO> items,
        long totalItems,
        int totalPages,
        int currentPages,
        int pageSize
) {
}
