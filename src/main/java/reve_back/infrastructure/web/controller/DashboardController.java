package reve_back.infrastructure.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reve_back.application.ports.in.GetBranchSalesUseCase;
import reve_back.application.ports.in.GetInventoryAlertsUseCase;
import reve_back.application.ports.in.GetWorkerPerformanceUseCase;
import reve_back.infrastructure.web.dto.BranchSalesReportResponse;
import reve_back.infrastructure.web.dto.InventoryAlertResponse;
import reve_back.infrastructure.web.dto.WorkerPerformanceResponse;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/reports")
public class DashboardController {

    private final GetBranchSalesUseCase getBranchSalesUseCase;
    private final GetWorkerPerformanceUseCase getWorkerPerformanceUseCase;
    private final GetInventoryAlertsUseCase getInventoryAlertsUseCase;

    @GetMapping("/branches")
    @PreAuthorize("hasAuthority('catalog:read:all')")
    public ResponseEntity<List<BranchSalesReportResponse>> getBranchReports(
            @RequestParam(value = "type", defaultValue = "monthly") String type
    ) {
        return ResponseEntity.ok(getBranchSalesUseCase.getBranchSales(type));
    }

    @GetMapping("/workers")
    @PreAuthorize("hasAuthority('catalog:read:all')")
    public ResponseEntity<List<WorkerPerformanceResponse>> getWorkerReports(
            @RequestParam(value = "type", defaultValue = "monthly") String type
    ) {
        return ResponseEntity.ok(getWorkerPerformanceUseCase.getWorkerPerformance(type));
    }

    @GetMapping("/inventory-alerts")
    @PreAuthorize("hasAuthority('catalog:read:all')")
    public ResponseEntity<List<InventoryAlertResponse>> getInventoryAlerts() {
        return ResponseEntity.ok(getInventoryAlertsUseCase.getInventoryAlerts());
    }
}
