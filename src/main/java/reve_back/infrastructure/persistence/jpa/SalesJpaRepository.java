package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import reve_back.infrastructure.persistence.entity.SaleEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface SalesJpaRepository extends JpaRepository<SaleEntity, Long> {
    // Suma total de todas las ventas de un cliente
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM SaleEntity s WHERE s.clientId = :clientId")
    BigDecimal sumTotalAmountByClientId(@Param("clientId") Long clientId);

    // Suma de ventas DESPUÉS de una fecha específica
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM SaleEntity s WHERE s.clientId = :clientId AND s.saleDate > :date")
    BigDecimal sumTotalAmountByClientIdAndDateAfter(@Param("clientId") Long clientId, @Param("date") LocalDateTime date);

}
