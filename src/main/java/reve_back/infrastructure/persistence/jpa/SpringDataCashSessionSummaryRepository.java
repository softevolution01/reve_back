package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import reve_back.infrastructure.persistence.entity.CashSessionsSummaryEntity;

import java.util.List;

@Repository
public interface SpringDataCashSessionSummaryRepository extends JpaRepository<CashSessionsSummaryEntity, Long> {
    List<CashSessionsSummaryEntity> findByCashSessionId(Long cashSessionId);
}
