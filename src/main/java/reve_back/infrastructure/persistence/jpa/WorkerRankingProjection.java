package reve_back.infrastructure.persistence.jpa;

import java.math.BigDecimal;

public interface WorkerRankingProjection {
    String getWorkerName();
    Long getTicketsCount();
    BigDecimal getTotalSold();
}
