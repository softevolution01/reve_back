package reve_back.infrastructure.web.dto;

public record DecantResponse(
        Long id,
        Integer volumeMl,
        Double price,
        String barcode,
        String barcodeImage
) {
}
