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

    @Query("SELECT SUM(m.amount) FROM CashMovementEntity m " +
            "WHERE m.cashSession.id = :sessionId " +
            "AND m.type = 'INGRESO' " +
            "AND m.method = 'EFECTIVO'") // <--- FILTRO CLAVE
    BigDecimal sumCashIncomeBySession(@Param("sessionId") Long sessionId);

    // 2. Sumar Egresos (SOLO EFECTIVO) para el saldo físico
    @Query("SELECT SUM(m.amount) FROM CashMovementEntity m " +
            "WHERE m.cashSession.id = :sessionId " +
            "AND m.type = 'EGRESO' " +
            "AND m.method = 'EFECTIVO'") // <--- FILTRO CLAVE
    BigDecimal sumCashExpenseBySession(@Param("sessionId") Long sessionId);

    // 3. (Opcional) Si quieres sumar Yape/Tarjeta para reportes
    @Query("SELECT SUM(m.amount) FROM CashMovementEntity m " +
            "WHERE m.cashSession.id = :sessionId AND m.method = :method")
    BigDecimal sumBySessionAndMethod(@Param("sessionId") Long sessionId, @Param("method") String method);
}
