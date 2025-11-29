package reve_back.infrastructure.web.dto;

import java.util.List;

public record ProductListResponse(
        Long id,
        String brand,
        String line,
        String concentration,
        Double price,
        Integer volumeProductsMl,
        List<BottleCreationResponse> bottles,
        List<DecantResponse> decants
) {
}
