package reve_back.application.ports.out;

import reve_back.infrastructure.web.dto.BranchSalesReportResponse;
import reve_back.infrastructure.web.dto.InventoryAlertResponse;
import reve_back.infrastructure.web.dto.WorkerPerformanceResponse;

import java.util.List;

public interface DashboardRepositoryPort {
    List<BranchSalesReportResponse> getSalesByBranch(String periodFormat);

    List<WorkerPerformanceResponse> getSalesByWorker(String periodFormat);

    List<InventoryAlertResponse> getInventoryAlerts(Double thresholdPercentage);
}
