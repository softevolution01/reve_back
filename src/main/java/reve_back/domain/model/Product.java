package reve_back.domain.model;

import java.time.LocalDateTime;

public record Product(
        Long id,
        String brand,
        Double price,
        String line,
        String concentration,
        Integer volumeProductsMl,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
