package reve_back.infrastructure.web.dto;

import java.util.List;

public record ProductCreationRequest(
        String brand,
        String line,
        String concentration,
        Double price,
        List<BottleCreationRequest> bottles
) {
}
