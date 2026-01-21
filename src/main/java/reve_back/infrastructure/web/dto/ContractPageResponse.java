package reve_back.infrastructure.web.dto;

import java.util.List;

public record ContractPageResponse(
        List<ContractListResponse> items,
        long totalItems,
        int totalPages,
        int currentPages,
        int pageSize
) {
}
