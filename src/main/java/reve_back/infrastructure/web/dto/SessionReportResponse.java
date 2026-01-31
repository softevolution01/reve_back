package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SessionReportResponse(
        Long id,
        String warehouseName,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        String openedBy, // Nombre del usuario
        String closedBy, // Nombre del usuario
        String status,   // OPEN, CLOSED
        BigDecimal initialCash,
        BigDecimal finalCashCounted,
        BigDecimal totalManualIncome,
        BigDecimal totalManualExpense,
        BigDecimal difference,
        String notes,
        List<SessionSummaryResponse> summaries,
        List<SessionMovementResponse> movements
) {}
