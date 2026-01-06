package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import reve_back.infrastructure.persistence.entity.SaleEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface SalesJpaRepository extends JpaRepository<SaleEntity, Long> {
    // Suma total de todas las ventas de un cliente
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM SaleEntity s WHERE s.client.id = :clientId")
    BigDecimal sumTotalAmountByClientId(@Param("clientId") Long clientId);

    // CORRECCIÓN 2: Suma total desde una fecha (para conteo anual o VIP)
    // Asumo que tu campo de fecha en SaleEntity se llama 'saleDate'
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM SaleEntity s WHERE s.client.id = :clientId AND s.saleDate > :date")
    BigDecimal sumTotalAmountByClientIdAndDateAfter(@Param("clientId") Long clientId, @Param("date") LocalDateTime date);

    @Query(value = """
        SELECT\s
            b.name AS "sucursal",
            TO_CHAR(s.sale_date, :periodFormat) AS "periodo",\s
            COALESCE(SUM(CASE WHEN si.decant_price_id IS NULL THEN si.final_subtotal ELSE 0 END), 0) AS "totalSellados",
            COALESCE(SUM(CASE WHEN si.decant_price_id IS NOT NULL THEN si.final_subtotal ELSE 0 END), 0) AS "totalDecants",
            COALESCE(SUM(si.final_subtotal), 0) AS "totalGeneral"
        FROM sales s
        JOIN sale_items si ON s.id = si.sale_id
        JOIN branches b ON s.branch_id = b.id
        GROUP BY b.name, "periodo"
        ORDER BY "periodo" DESC, "totalGeneral" DESC
   \s""", nativeQuery = true)
    List<Map<String, Object>> getSalesByBranchRaw(@Param("periodFormat") String periodFormat);

    // 2. REPORTE DE TRABAJADORES (Usando Map)
    @Query(value = """
        SELECT\s
            u.fullname AS "trabajador",
            TO_CHAR(s.sale_date, :periodFormat) AS "periodo",
            COUNT(s.id) AS "cantidadTickets",
            COALESCE(SUM(s.total_final_charged), 0) AS "totalVendido"
        FROM sales s
        JOIN users u ON s.user_id = u.id
        GROUP BY u.fullname, "periodo"
        ORDER BY "periodo" DESC, "totalVendido" DESC
   \s""", nativeQuery = true)
    List<Map<String, Object>> getSalesByWorkerRaw(@Param("periodFormat") String periodFormat);

}
