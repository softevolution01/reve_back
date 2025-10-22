package reve_back.application.ports.out;

import org.springframework.data.domain.Page;
import reve_back.domain.model.NewProduct;
import reve_back.domain.model.Product;
import reve_back.infrastructure.persistence.entity.ProductEntity;
import reve_back.infrastructure.web.dto.ProductSummaryDTO;

import java.util.List;

public interface ProductRepositoryPort {
    Product save(NewProduct product);
    Page<ProductSummaryDTO> findAll(int page, int size);
    ProductEntity findById(Long id);
    ProductEntity update(ProductEntity productEntity);
    void setInactiveById(Long id);
    boolean existsByBrandAndLine(String brand, String lines);
    boolean existsByBrandAndLineAndIdNot(String brand, String lines, Long id);
}
