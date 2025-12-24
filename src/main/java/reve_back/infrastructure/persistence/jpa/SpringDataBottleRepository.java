package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reve_back.domain.model.BottlesStatus;
import reve_back.infrastructure.persistence.entity.BottleEntity;

import java.util.List;
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
}
