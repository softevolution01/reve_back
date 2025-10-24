package reve_back.domain.model;

public record NewProduct(
        String brand,
        String line,
        String concentration,
        Double price
) {
}
