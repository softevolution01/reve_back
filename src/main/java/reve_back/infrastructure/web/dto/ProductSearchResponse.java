package reve_back.infrastructure.web.dto;

public record ProductSearchResponse(
        Long idInventario,
        String tipoVendible, // "BOTELLA" o "DECANT"
        Long productoId,
        String nombreProducto,
        String line,
        Integer volumeMl,
        Double price
) {}
