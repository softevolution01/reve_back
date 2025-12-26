package reve_back.domain.model;

import java.time.LocalDateTime;

public record Product(
        Long id,
        String brand,
        String line,
        String concentration,
        Double price,
        Integer volumeProductsMl,
        Boolean isActive,
        Boolean allowPromotions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
