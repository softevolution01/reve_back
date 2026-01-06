package reve_back.infrastructure.web.dto;

import java.math.BigDecimal;

public record BranchSalesReportResponse(
        String branchName,
        String period,
        BigDecimal totalSealed,
        BigDecimal totalDecants,
        BigDecimal totalGeneral,
        String status // "ALERTA" o "NORMAL"
) {}
