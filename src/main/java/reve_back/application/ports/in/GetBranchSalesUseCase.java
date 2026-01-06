package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.BranchSalesReportResponse;

import java.util.List;

public interface GetBranchSalesUseCase {
    List<BranchSalesReportResponse> getBranchSales(String periodType);
}
