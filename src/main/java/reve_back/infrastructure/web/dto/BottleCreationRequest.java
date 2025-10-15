package reve_back.infrastructure.web.dto;

public record BottleCreationRequest(
        String barcode,
        Long branchId,
        Integer volumeMl,
        Integer remainingVolumeMl,
        String status
) {
}
