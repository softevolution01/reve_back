package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import reve_back.infrastructure.persistence.entity.SaleEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface SalesJpaRepository extends JpaRepository<SaleEntity, Long> {
    // Suma total de todas las ventas de un cliente
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM SaleEntity s WHERE s.client.id = :clientId")
    BigDecimal sumTotalAmountByClientId(@Param("clientId") Long clientId);

    // CORRECCIÓN 2: Suma total desde una fecha (para conteo anual o VIP)
    // Asumo que tu campo de fecha en SaleEntity se llama 'saleDate'
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM SaleEntity s WHERE s.client.id = :clientId AND s.saleDate > :date")
    BigDecimal sumTotalAmountByClientIdAndDateAfter(@Param("clientId") Long clientId, @Param("date") LocalDateTime date);

}
