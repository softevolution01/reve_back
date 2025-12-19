package reve_back.infrastructure.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reve_back.application.ports.in.GetDailyReportUseCase;
import reve_back.infrastructure.web.dto.DailyReportResponse;

@RequiredArgsConstructor
@RestController
@RequestMapping("/reports")
public class ReportController {
    private final GetDailyReportUseCase reportUseCase;

    @GetMapping("/daily-summary")
    @PreAuthorize("hasAuthority('menu:inventory:access')")
    public ResponseEntity<DailyReportResponse> getDailySummary() {
        return ResponseEntity.ok(reportUseCase.getSummary());
    }
}
