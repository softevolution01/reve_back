package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.ProductRepositoryPort;
import reve_back.domain.model.DecantPrice;
import reve_back.domain.model.NewProduct;
import reve_back.domain.model.Product;
import reve_back.infrastructure.persistence.entity.DecantPriceEntity;
import reve_back.infrastructure.persistence.entity.ProductEntity;
import reve_back.infrastructure.persistence.jpa.SpringDataDecantPriceRepository;
import reve_back.infrastructure.persistence.jpa.SpringDataProductRepository;
import reve_back.infrastructure.web.dto.ProductSummaryDTO;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class JpaProductRepositoryAdapter implements ProductRepositoryPort {

    private final SpringDataProductRepository springDataProductRepository;
    private final SpringDataDecantPriceRepository springDataDecantPriceRepository;

    @Override
    public Product save(NewProduct product) {
        ProductEntity entity = new ProductEntity();

        entity.setBrand(product.brand());
        entity.setLine(product.line());
        entity.setConcentration(product.concentration());
        entity.setPrice(product.price());
        entity.set_active(true);
        entity.setVolumeProductsMl(product.unitVolumeMl());
        entity.setCreatedAt(java.time.LocalDateTime.now());
        entity.setUpdatedAt(java.time.LocalDateTime.now());

        ProductEntity savedEntity = springDataProductRepository.save(entity);


        return new Product(
                savedEntity.getId(),
                savedEntity.getBrand(),
                savedEntity.getLine(),
                savedEntity.getConcentration(),
                savedEntity.getPrice());
    }

    @Override
    public Page<ProductSummaryDTO> findAll(int page, int size) {

        Page<ProductEntity> productPage = springDataProductRepository.findByIsActiveTrue(PageRequest.of(page, size));

        List<ProductSummaryDTO> items = productPage.getContent().stream()
                .map(entity -> new ProductSummaryDTO(
                        entity.getId(),
                        entity.getBrand(),
                        entity.getLine(),
                        entity.getConcentration(),
                        entity.getPrice(),
                        entity.getVolumeProductsMl()))
                .collect(Collectors.toList());

        return new PageImpl<>(items, PageRequest.of(page, size), productPage.getTotalElements());
    }

    @Override
    public ProductEntity findById(Long id) {
        return springDataProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    @Override
    public ProductEntity update(ProductEntity productEntity) {
        return springDataProductRepository.save(productEntity);
    }

    @Override
    public void setInactiveById(Long id) {
        ProductEntity productEntity = findById(id);
        productEntity.set_active(false);
        springDataProductRepository.save(productEntity);
    }

    @Override
    public boolean existsByBrandAndLine(String brand, String lines) {
        return springDataProductRepository.existsByBrandAndLine(brand, lines);
    }

    @Override
    public boolean existsByBrandAndLineAndIdNot(String brand, String lines, Long id) {
        return springDataProductRepository.existsByBrandAndLineAndIdNot(brand, lines, id);
    }

    @Override
    public List<DecantPriceEntity> findAllByProductId(Long productId) {
        return springDataDecantPriceRepository.findByProductId(productId);
    }

    @Override
    public boolean existsByBrandLineAndVolumeProductsMl(String brand, String line, Integer unitVolumeMl) {
        return springDataProductRepository.existsByBrandAndLineAndVolumeProductsMl(brand, line, unitVolumeMl);
    }

    @Override
    public boolean existsByBrandLineAndVolumeProductsMlAndIdNot(String brand, String line, Integer unitVolumeMl, Long id) {
        return springDataProductRepository.existsByBrandAndLineAndVolumeProductsMlAndIdNot(brand, line, unitVolumeMl, id);
    }
}
