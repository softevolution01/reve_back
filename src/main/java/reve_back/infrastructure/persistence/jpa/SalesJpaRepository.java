package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import reve_back.infrastructure.persistence.entity.SaleEntity;
import reve_back.infrastructure.web.dto.WorkerRankingResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface SalesJpaRepository extends JpaRepository<SaleEntity, Long> {
    // Suma total de todas las ventas de un cliente
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM SaleEntity s WHERE s.client.id = :clientId")
    BigDecimal sumTotalAmountByClientId(@Param("clientId") Long clientId);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM SaleEntity s WHERE s.client.id = :clientId AND s.saleDate > :date")
    BigDecimal sumTotalAmountByClientIdAndDateAfter(@Param("clientId") Long clientId, @Param("date") LocalDateTime date);

    @Query("SELECT " +
            "   u.fullname AS workerName, " +
            "   COUNT(s) AS ticketsCount, " +
            // CORRECCIÓN 1: s.total -> s.totalAmount
            "   COALESCE(SUM(s.totalAmount), 0) AS totalSold " +
            "FROM SaleEntity s " +
            "JOIN s.user u " +
            // CORRECCIÓN 2: s.createdAt -> s.saleDate
            "WHERE s.saleDate BETWEEN :start AND :end " +
            // CORRECCIÓN 3: He quitado 's.status' porque NO existe en tu SaleEntity
            // "AND s.status = 'COMPLETED' " +
            "GROUP BY u.fullname " +
            "ORDER BY totalSold DESC")
    List<WorkerRankingProjection> getWorkerRankingByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
