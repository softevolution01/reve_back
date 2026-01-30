package reve_back.infrastructure.web.dto;

import reve_back.domain.model.CashMovement;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CashStatusResponse(
        String status,
        Long sessionId,             // 1
        BigDecimal initialCash,     // 2
        BigDecimal totalSalesCash,  // 3
        BigDecimal totalIncome,     // 4
        BigDecimal totalExpense,    // 5
        BigDecimal currentSystemBalance, // 6
        Map<String, BigDecimal> paymentBreakdown,
        List<CashMovement> movements // 7
) {}
