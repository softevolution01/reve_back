package reve_back.domain.model;

public record DecantPrice(
        Long id,
        Integer volumeMl,
        Double price,
        String barcode
) {
}
