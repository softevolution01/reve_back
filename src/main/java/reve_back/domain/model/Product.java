package reve_back.domain.model;

public record Product(
        Long id,
        String brand,
        String line,
        String concentration,
        Double price
) {
}
