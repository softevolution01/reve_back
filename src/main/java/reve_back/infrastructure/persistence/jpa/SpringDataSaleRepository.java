package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reve_back.infrastructure.persistence.entity.SaleEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RepositoryRestResource(exported = false)
public interface SpringDataSaleRepository extends JpaRepository<SaleEntity, Long> {

    // Método extra: Buscar ventas por rango de fechas (útil para cierre de caja)
    // Spring genera el SQL automáticamente basado en el nombre del método
    List<SaleEntity> findAllBySaleDateBetween(LocalDateTime start, LocalDateTime end);

    // Método extra: Buscar ventas de un usuario específico en una fecha
    @Query("SELECT s FROM SaleEntity s WHERE s.user.id = :userId AND s.saleDate BETWEEN :start AND :end")
    List<SaleEntity> findByUserAndDateRange(@Param("userId") Long userId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(s.totalFinalCharged), 0) FROM SaleEntity s WHERE s.cashSession.id = :sessionId")
    BigDecimal sumTotalByCashSessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT pm.name as method, SUM(sp.amount) as total " +
            "FROM SalePaymentEntity sp " +
            "JOIN sp.paymentMethod pm " +
            "JOIN sp.sale s " +
            "WHERE s.cashSession.id = :sessionId " +
            "GROUP BY pm.name")
    List<PaymentMethodSummary> getPaymentBreakdownBySession(@Param("sessionId") Long sessionId);
}
