package reve_back.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record SaleSimulationRequest(
        @JsonProperty("promotionId")
        Long promotionId,
        List<CartItemRequest> items // Ítems tal cual vienen del frontend
) {}
