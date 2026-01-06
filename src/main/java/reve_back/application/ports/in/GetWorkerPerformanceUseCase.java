package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.WorkerPerformanceResponse;

import java.util.List;

public interface GetWorkerPerformanceUseCase {
    List<WorkerPerformanceResponse> getWorkerPerformance(String periodType);
}
