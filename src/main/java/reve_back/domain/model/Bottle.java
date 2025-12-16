package reve_back.domain.model;

public record Bottle(
        Long id,
        Long productId,
        Long warehouseId,
        String status,
        String barcode,
        Integer volumeMl,
        Integer remainingVolumeMl,
        Integer quantity
) {
}
