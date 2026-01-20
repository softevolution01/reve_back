package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractListResponse(
        Long id,
        String clientName,
        String productName, // Nombre Botella
        Integer quantity,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal priceBase,
        BigDecimal priceWithDiscount, // Precio Final
        BigDecimal advancePayment,
        BigDecimal pendingBalance,
        String status
) {
}
