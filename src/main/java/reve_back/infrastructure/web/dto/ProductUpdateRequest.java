package reve_back.infrastructure.web.dto;

import java.util.List;

public record ProductUpdateRequest(
        String brand,
        String line,
        String concentration,
        Double price,
        Integer unitVolumeMl,
        List<BottleCreationRequest> bottles
) {
}
