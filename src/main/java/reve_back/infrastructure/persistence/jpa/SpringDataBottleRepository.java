package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reve_back.domain.model.Bottle;
import reve_back.domain.model.BottlesStatus;
import reve_back.infrastructure.persistence.entity.BottleEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RepositoryRestResource(exported = false)
public interface SpringDataBottleRepository extends JpaRepository<BottleEntity,Long> {
    List<BottleEntity> findByProductId(Long productId);
    Optional<BottleEntity> findByBarcodeAndStatus(String barcode, BottlesStatus status);
    @Query("""
        SELECT b\s
        FROM BottleEntity b
        WHERE b.status = 'SELLADA'
          AND b.quantity > 0
          AND (LOWER(b.product.brand) LIKE LOWER(CONCAT('%', :term, '%'))\s
               OR LOWER(b.product.line) LIKE LOWER(CONCAT('%', :term, '%')))
   \s""")
    List<BottleEntity> findActiveByProductNameLike(@Param("term") String term, Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(b.remainingVolumeMl * b.quantity), 0)
        FROM BottleEntity b
        WHERE b.product.id = :productId
          AND b.status IN ('SELLADA', 'DECANTADA')
    """)
    Integer sumTotalStockByProduct(@Param("productId") Long productId);

    @Query(value = """
        SELECT\s
            w.name AS "almacen",
            p.brand || ' - ' || p.line AS "producto",
            b.barcode AS "barcode",
            b.volume_ml AS "capacidad",
            b.remaining_volume_ml AS "restante",
            ROUND((b.remaining_volume_ml\\:\\:numeric / b.volume_ml\\:\\:numeric) * 100, 1) AS "porcentaje"
        FROM bottles b
        JOIN products p ON b.product_id = p.id
        JOIN warehouses w ON b.warehouse_id = w.id
        WHERE b.status = 'DECANTADA'\s
          AND b.volume_ml > 0
          AND (b.remaining_volume_ml\\:\\:numeric / b.volume_ml\\:\\:numeric) <= :threshold
        ORDER BY "porcentaje" ASC
   \s""", nativeQuery = true)
    List<Map<String, Object>> getInventoryAlertsRaw(@Param("threshold") Double threshold);

    @Query("SELECT b FROM BottleEntity b " +
            "WHERE b.product.id = :productId " +
            "AND b.warehouse.id = :warehouseId " +
            "AND b.status = 'SELLADA' " +
            "AND b.barcode IS NOT NULL ")
    Optional<BottleEntity> findSellableBottleForSale(@Param("productId") Long productId,
                                                     @Param("warehouseId") Long warehouseId);
}
