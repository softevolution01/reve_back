package reve_back.infrastructure.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reve_back.application.ports.in.DashboardUseCase;
import reve_back.infrastructure.web.dto.InventoryAlertResponse;
import reve_back.infrastructure.web.dto.SessionReportResponse;
import reve_back.infrastructure.web.dto.WorkerRankingResponse;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/reports")
public class DashboardController {

    private final DashboardUseCase dashboardUseCase;

    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('catalog:read:all')")
    public ResponseEntity<List<SessionReportResponse>> getSessionHistory(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ResponseEntity.ok(dashboardUseCase.getSessionHistory(start, end));
    }

    @GetMapping("/ranking")
    @PreAuthorize("hasAuthority('catalog:read:all')")
    public ResponseEntity<List<WorkerRankingResponse>> getWorkerRanking(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ResponseEntity.ok(dashboardUseCase.getWorkerRanking(start, end));
    }

    @GetMapping("/inventory-alerts")
    @PreAuthorize("hasAuthority('catalog:read:all')")
    public ResponseEntity<List<InventoryAlertResponse>> getInventoryAlerts() {
        return ResponseEntity.ok(dashboardUseCase.getInventoryAlerts());
    }

}
