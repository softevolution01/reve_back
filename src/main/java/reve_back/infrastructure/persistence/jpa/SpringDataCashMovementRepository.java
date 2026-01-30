package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reve_back.domain.model.CashMovement;
import reve_back.infrastructure.persistence.entity.CashMovementEntity;
import reve_back.infrastructure.persistence.enums.global.CashMovementType;

import java.math.BigDecimal;
import java.util.List;

@RepositoryRestResource(exported = false)
public interface SpringDataCashMovementRepository extends JpaRepository<CashMovementEntity, Long> {
    void save(CashMovement cashMovement);
    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM CashMovementEntity m WHERE m.cashSession.id = :sessionId AND m.type = :type")
    BigDecimal sumAmountBySessionAndType(@Param("sessionId") Long sessionId, @Param("type") CashMovementType type);

    List<CashMovementEntity> findTop10ByCashSessionIdOrderByCreatedAtDesc(Long sessionId);

    // Usamos COALESCE(m.method, 'EFECTIVO') para que si es NULL, lo cuente como efectivo
    @Query("SELECT COALESCE(m.method, 'EFECTIVO') as method, SUM(m.amount) as total " +
            "FROM CashMovementEntity m " +
            "WHERE m.cashSession.id = :sessionId " +
            "AND m.type = 'VENTA' " +
            "GROUP BY COALESCE(m.method, 'EFECTIVO')") // <--- Importante agrupar igual
    List<PaymentMethodSummary> getVentaBreakdownBySession(@Param("sessionId") Long sessionId);

    // 2. Sumar Ingresos Manuales (Caja chica, sencillos) - Tipo INGRESO
    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM CashMovementEntity m " +
            "WHERE m.cashSession.id = :sessionId AND m.type = 'INGRESO'")
    BigDecimal sumTotalIncomeBySession(@Param("sessionId") Long sessionId);

    // 3. Sumar Egresos Manuales (Gastos) - Tipo EGRESO
    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM CashMovementEntity m " +
            "WHERE m.cashSession.id = :sessionId AND m.type = 'EGRESO'")
    BigDecimal sumTotalExpenseBySession(@Param("sessionId") Long sessionId);
}
