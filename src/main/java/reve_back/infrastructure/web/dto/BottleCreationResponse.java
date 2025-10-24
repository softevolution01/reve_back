package reve_back.infrastructure.web.dto;

public record BottleCreationResponse(
        Long id,
        String barcode,
        Long branchId,
        Integer volumeMl,
        Integer remainingVolumeMl,
        Integer quantity,
        String status,
        String barcodeImage
) {
}
