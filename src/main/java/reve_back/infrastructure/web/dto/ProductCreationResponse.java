package reve_back.infrastructure.web.dto;

import reve_back.domain.model.Bottle;

import java.util.List;

public record ProductCreationResponse(
        Long id,
        String brand,
        String line,
        String concentration,
        Double price,
        List<BottleCreationResponse> bottles,
        List<DecantResponse> decants
) {
}
