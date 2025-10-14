package reve_back.infrastructure.web.dto;

public record ProductSummaryDTO(
        Long id,
        String brand,
        String line,
        String concentration,
        Double price
) {
}
