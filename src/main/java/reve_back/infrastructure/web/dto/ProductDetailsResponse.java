package reve_back.infrastructure.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProductDetailsResponse(
        Long id,
        String brand,
        String line,
        String concentration,
        Double price,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<BottleCreationResponse> bottles
) {
}
