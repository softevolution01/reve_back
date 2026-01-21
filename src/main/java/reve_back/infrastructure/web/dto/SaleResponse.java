package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleResponse(
        Long saleId,
        LocalDateTime date,
        String branchName,
        String sellerName, // Sugerencia extra: devolver nombre del vendedor en vez de ID
        String clientName, // <--- AGREGAR ESTO (Importante)
        String clientDNI,
        BigDecimal totalBruto,
        BigDecimal totalDiscount,
        BigDecimal totalSurcharge,
        BigDecimal totalNeto,
        BigDecimal baseImponibleIGV,
        String paymentMethodName,
        List<SaleItemResponse> items
) {}
