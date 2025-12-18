package reve_back.domain.model;

import java.math.BigDecimal;

public record LoyaltyTiers (
        Integer tierLevel,
        Double costPerPoint
){
}
