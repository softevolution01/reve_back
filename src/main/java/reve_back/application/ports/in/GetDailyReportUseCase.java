package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.DailyReportResponse;

public interface GetDailyReportUseCase {
    DailyReportResponse getSummary();
}
