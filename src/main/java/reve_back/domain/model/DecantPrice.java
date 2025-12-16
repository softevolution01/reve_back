package reve_back.domain.model;

public record DecantPrice(
        Long id,
        Long productId,
        Integer volumeMl,
        Double price,
        String barcode,
        String imageBarcode
) {
}
