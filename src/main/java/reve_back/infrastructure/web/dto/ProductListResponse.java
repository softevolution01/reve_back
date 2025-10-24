package reve_back.infrastructure.web.dto;

import java.util.List;

public record ProductListResponse(
        Long id,
        String brand,
        String line,
        String concentration,
        Double price,
        List<BottleCreationResponse> bottles
) {
}
