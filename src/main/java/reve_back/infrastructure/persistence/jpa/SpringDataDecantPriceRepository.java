package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.domain.Pageable;
import reve_back.infrastructure.persistence.entity.DecantPriceEntity;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(exported = false)
public interface SpringDataDecantPriceRepository extends JpaRepository<DecantPriceEntity, Long> {
    List<DecantPriceEntity> findByProductId(Long productId);

    Optional<DecantPriceEntity> findByBarcode(String barcode);

    @Query("""
                SELECT d\s
                FROM DecantPriceEntity d
                WHERE\s
                  (LOWER(d.product.brand) LIKE LOWER(CONCAT('%', :term, '%'))\s
                   OR LOWER(d.product.line) LIKE LOWER(CONCAT('%', :term, '%')))
                  AND EXISTS (
                      SELECT 1 FROM BottleEntity b\s
                      WHERE b.product.id = d.product.id\s
                        AND b.remainingVolumeMl >= d.volumeMl
                  )
           \s""")
    List<DecantPriceEntity> findActiveByProductNameLike(@Param("term") String term, Pageable pageable);
    @Query("SELECT d.barcode FROM DecantPriceEntity d WHERE d.barcode LIKE CONCAT(:prefix, '%') ORDER BY d.barcode DESC")
    List<String> findLastBarcode(@Param("prefix") String prefix, Pageable pageable);
}
