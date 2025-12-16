package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reve_back.domain.model.NewProduct;
import reve_back.domain.model.Product;
import reve_back.infrastructure.persistence.entity.ProductEntity;

@RepositoryRestResource(exported = false)
public interface SpringDataProductRepository extends JpaRepository<ProductEntity,Long> {

    Page<ProductEntity> findAll(Pageable pageable);
    boolean existsByBrandAndLine(String brand, String lines);
    boolean existsByBrandAndLineAndIdNot(String brand, String lines,Long id);
    @Query("SELECT p FROM ProductEntity p WHERE p.is_active = true")
    Page<ProductEntity> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT COUNT(p) FROM ProductEntity p WHERE p.is_active = true")
    long countByIsActiveTrue();

    boolean existsByBrandIgnoreCaseAndLineIgnoreCaseAndConcentrationIgnoreCaseAndVolumeProductsMl(
            String brand, String line, String concentration, Integer unitVolumeMl);

    boolean existsByBrandIgnoreCaseAndLineIgnoreCaseAndConcentrationIgnoreCaseAndVolumeProductsMlAndIdNot(
            String brand, String line, String concentration, Integer unitVolumeMl, Long id);
}
