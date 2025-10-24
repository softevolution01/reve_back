package reve_back.infrastructure.web.dto;

import java.util.List;

public record ProductPageResponse(
        List<ProductListResponse> items,
        long totalItems,
        int totalPages,
        int currentPages,
        int pageSize
) {
}
