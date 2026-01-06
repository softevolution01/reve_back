package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reve_back.application.ports.in.GetBranchSalesUseCase;
import reve_back.application.ports.in.GetInventoryAlertsUseCase;
import reve_back.application.ports.in.GetWorkerPerformanceUseCase;
import reve_back.application.ports.out.DashboardRepositoryPort;
import reve_back.infrastructure.web.dto.BranchSalesReportResponse;
import reve_back.infrastructure.web.dto.InventoryAlertResponse;
import reve_back.infrastructure.web.dto.WorkerPerformanceResponse;

import java.util.List;

@RequiredArgsConstructor
@Service
public class DashboardService implements
        GetBranchSalesUseCase,
        GetWorkerPerformanceUseCase,
        GetInventoryAlertsUseCase {

    private final DashboardRepositoryPort dashboardRepositoryPort;

    @Override
    public List<BranchSalesReportResponse> getBranchSales(String periodType) {
        // Convertimos "weekly"/"monthly" al formato de Postgres
        String format = "weekly".equalsIgnoreCase(periodType) ? "YYYY-IW" : "YYYY-MM";
        return dashboardRepositoryPort.getSalesByBranch(format);
    }

    @Override
    public List<WorkerPerformanceResponse> getWorkerPerformance(String periodType) {
        String format = "weekly".equalsIgnoreCase(periodType) ? "YYYY-IW" : "YYYY-MM";
        return dashboardRepositoryPort.getSalesByWorker(format);
    }

    @Override
    public List<InventoryAlertResponse> getInventoryAlerts() {
        // Definimos la regla de negocio del 40% aquí
        return dashboardRepositoryPort.getInventoryAlerts(0.40);
    }
}
