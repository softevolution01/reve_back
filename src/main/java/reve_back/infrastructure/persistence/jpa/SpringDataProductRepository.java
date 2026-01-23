package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reve_back.infrastructure.persistence.entity.ProductEntity;

import java.util.List;
import java.util.Map;


@RepositoryRestResource(exported = false)
public interface SpringDataProductRepository extends JpaRepository<ProductEntity,Long> {

    Page<ProductEntity> findAll(Pageable pageable);
    boolean existsByBrandAndLine(String brand, String lines);
    boolean existsByBrandAndLineAndIdNot(String brand, String lines,Long id);
    @Query("SELECT p FROM ProductEntity p WHERE p.isActive = true")
    Page<ProductEntity> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT COUNT(p) FROM ProductEntity p WHERE p.isActive = true")
    long countByIsActiveTrue();

    boolean existsByBrandIgnoreCaseAndLineIgnoreCaseAndConcentrationIgnoreCaseAndVolumeProductsMl(
            String brand, String line, String concentration, Integer unitVolumeMl);

    boolean existsByBrandIgnoreCaseAndLineIgnoreCaseAndConcentrationIgnoreCaseAndVolumeProductsMlAndIdNot(
            String brand, String line, String concentration, Integer unitVolumeMl, Long id);

    @Query(value = """
        /* 1. DECANTS */
        SELECT\s
            UPPER(p.brand || ' ' || COALESCE(p.line, '') || ' ' || COALESCE(p.concentration, '')) as nombre_completo,
            'DECANT ' || d.volume_ml || 'ml' as detalle,
            'D' || LPAD(CAST(d.id AS TEXT), 4, '0') as codigo_visual,
            d.price as precio,
            p.brand, p.line, d.volume_ml
        FROM decant_prices d
        JOIN products p ON d.product_id = p.id
       \s
        UNION ALL
       \s
        /* 2. BOTELLAS (SELLADAS) */
        SELECT\s
            UPPER(p.brand || ' ' || COALESCE(p.line, '') || ' ' || COALESCE(p.concentration, '')) as nombre_completo,
            'BOTELLA ' || b.volume_ml || 'ml' as detalle,
            'B' || LPAD(CAST(b.id AS TEXT), 4, '0') as codigo_visual,
            p.price as precio,
            p.brand, p.line, b.volume_ml
        FROM bottles b
        JOIN products p ON b.product_id = p.id
        WHERE b.status = 'SELLADA'
       \s
        ORDER BY brand, line, volume_ml
   \s""", nativeQuery = true)
    List<Map<String, Object>> findAllLabelItemsRaw();

    @Query("SELECT p FROM ProductEntity p WHERE " +
            "(LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.line) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND p.isActive = true")
    Page<ProductEntity> searchByQuery(@Param("query") String query, Pageable pageable);
}
