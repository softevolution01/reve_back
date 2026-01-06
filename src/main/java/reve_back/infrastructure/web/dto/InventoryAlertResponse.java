package reve_back.infrastructure.web.dto;

public record InventoryAlertResponse(
        String warehouseName,
        String productName,
        String barcode,
        Integer capacity,
        Integer remaining,
        Double percentage
) {}