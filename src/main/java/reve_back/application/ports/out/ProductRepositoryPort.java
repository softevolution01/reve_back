package reve_back.application.ports.out;

import org.springframework.data.domain.Page;
import reve_back.domain.model.NewProduct;
import reve_back.domain.model.Product;
import reve_back.infrastructure.persistence.entity.DecantPriceEntity;
import reve_back.infrastructure.persistence.entity.ProductEntity;
import reve_back.infrastructure.web.dto.LabelItemDTO;
import reve_back.infrastructure.web.dto.ProductSummaryDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProductRepositoryPort {
    Product save(Product product);
    Page<ProductSummaryDTO> findAll(int page, int size);
    Optional<Product> findById(Long id);
    void setInactiveById(Long id);

    boolean existsByBrandAndLineAndConcentrationAndVolumeProductsMl(String brand, String line,String concentration, Integer unitVolumeMl);
    boolean existsByBrandAndLineAndConcentrationAndVolumeProductsMlAndIdNot(String brand, String line, String concentration,Integer unitVolumeMl, Long id);

    List<LabelItemDTO> getLabelCatalog();

}
