package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleResponse(
        Long saleId,
        LocalDateTime date,
        String branchName,
        String sellerName,
        String clientName,

        BigDecimal subtotalBruto,
        BigDecimal totalDiscount, // Suma de promo + manual
        BigDecimal totalSurcharge, // Comisiones tarjeta
        BigDecimal totalNeto, // Lo que pagó el cliente
        BigDecimal baseImponibleIGV, // Para SUNAT

        List<SaleItemResponse> items
) {}
