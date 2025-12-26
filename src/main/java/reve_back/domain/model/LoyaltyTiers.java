package reve_back.domain.model;

import java.math.BigDecimal;

public record LoyaltyTiers (
        Long id,
        Integer tierLevel,
        BigDecimal costPerPoint
){
}
