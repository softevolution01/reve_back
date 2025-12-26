package reve_back.infrastructure.web.dto;

public record BottleCreationResponse(
        Long id,
        String barcode,
        String warehouse,
        Integer volumeMl,
        Integer remainingVolumeMl,
        Integer quantity,
        String status,
        String barcodeImage
) {
}
