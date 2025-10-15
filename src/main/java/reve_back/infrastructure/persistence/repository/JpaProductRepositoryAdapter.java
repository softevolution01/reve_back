package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.ProductRepositoryPort;
import reve_back.domain.model.NewProduct;
import reve_back.domain.model.Product;
import reve_back.infrastructure.persistence.entity.ProductEntity;
import reve_back.infrastructure.persistence.jpa.SpringDataProductRepository;
import reve_back.infrastructure.web.dto.ProductSummaryDTO;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class JpaProductRepositoryAdapter implements ProductRepositoryPort {

    private final SpringDataProductRepository springDataProductRepository;

    @Override
    public Product save(NewProduct product) {
        ProductEntity entity = new ProductEntity(
                        null, product.brand(), product.line(), product.concentration(),
                        product.price(), null, null, product.unitVolumeMl()
                );
        ProductEntity savedEntity = springDataProductRepository.save(entity);
        return new Product(
                savedEntity.getId(),
                savedEntity.getBrand(),
                savedEntity.getLine(),
                savedEntity.getConcentration(),
                savedEntity.getPrice());
    }

    @Override
    public List<ProductSummaryDTO> findAll(int page, int size) {
        Page<ProductEntity> productPage = springDataProductRepository.findAll(PageRequest.of(page,size));
        return productPage.getContent().stream()
                .map(entity -> new ProductSummaryDTO(
                        entity.getId(),
                        entity.getBrand(),
                        entity.getLine(),
                        entity.getConcentration(),
                        entity.getPrice()
                ))
                .collect(Collectors.toList());
    }
}
