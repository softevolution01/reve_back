package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reve_back.application.ports.in.DashboardUseCase;
import reve_back.application.ports.in.GetInventoryAlertsUseCase;
import reve_back.application.ports.out.DashboardRepositoryPort;
import reve_back.infrastructure.web.dto.BranchSalesReportResponse;
import reve_back.infrastructure.web.dto.InventoryAlertResponse;
import reve_back.infrastructure.web.dto.SessionReportResponse;
import reve_back.infrastructure.web.dto.WorkerRankingResponse;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class DashboardService implements DashboardUseCase{

    private final DashboardRepositoryPort dashboardRepositoryPort;

    @Override
    public List<SessionReportResponse> getSessionHistory(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha fin.");
        }
        return dashboardRepositoryPort.getSessionHistory(start, end);
    }

    @Override
    public List<WorkerRankingResponse> getWorkerRanking(LocalDateTime start, LocalDateTime end) {
        return dashboardRepositoryPort.getWorkerRanking(start, end);
    }

    @Override
    public List<InventoryAlertResponse> getInventoryAlerts() {
        return dashboardRepositoryPort.getInventoryAlerts(40.0);
    }
}
