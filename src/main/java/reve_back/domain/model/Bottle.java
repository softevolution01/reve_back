package reve_back.domain.model;

public record Bottle(
        Long id,
        Long productId,
        String status,
        String barcode,
        Integer volumeMl,
        Integer remainingVolumeMl,
        Integer quantity,
        Long warehouseId
) {
}
