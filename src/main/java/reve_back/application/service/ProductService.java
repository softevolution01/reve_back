package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.in.CreateProductUseCase;
import reve_back.application.ports.in.GetProductDetailsUseCase;
import reve_back.application.ports.in.ListProductsUseCase;
import reve_back.application.ports.out.BottleRepositoryPort;
import reve_back.application.ports.out.ProductRepositoryPort;
import reve_back.domain.exception.DuplicateBarcodeException;
import reve_back.domain.model.Bottle;
import reve_back.domain.model.NewProduct;
import reve_back.domain.model.Product;
import reve_back.infrastructure.persistence.entity.ProductEntity;
import reve_back.infrastructure.persistence.jpa.SpringDataProductRepository;
import reve_back.infrastructure.persistence.repository.JpaProductRepositoryAdapter;
import reve_back.infrastructure.web.dto.*;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ProductService implements ListProductsUseCase, CreateProductUseCase, GetProductDetailsUseCase {


    private final ProductRepositoryPort productRepositoryPort;
    private final BottleRepositoryPort bottleRepositoryPort;

    @Override
    public ProductCreationResponse createProduct(ProductCreationRequest request) {
        try {
            // Crea un nuevo producto
            NewProduct newProduct = new NewProduct(request.brand(), request.line(), request.concentration(),
                    request.price(), request.unitVolumeMl());
            Product savedProduct = productRepositoryPort.save(newProduct);

            // Crea botellas asociadas
            List<Bottle> bottles = request.bottles().stream()
                    .map(bottle -> new Bottle(
                            null,
                            savedProduct.id(),
                            bottle.status(),
                            bottle.barcode(),
                            bottle.volumeMl(),
                            bottle.remainingVolumeMl(),
                            bottle.branchId()
                    ))
                    .collect(Collectors.toList());
            List<Bottle> savedBottles = bottleRepositoryPort.saveAll(bottles);

            // Mapear respuesta
            List<BottleCreationResponse> bottleResponse = savedBottles.stream()
                    .map(b -> new BottleCreationResponse(
                            b.id(),
                            b.barcode(),
                            b.branchId(),
                            b.volumeMl(),
                            b.remainingVolumeMl(),
                            b.status()))
                    .collect(Collectors.toList());
            return new ProductCreationResponse(
                    savedProduct.id(),
                    savedProduct.brand(),
                    savedProduct.line(),
                    savedProduct.concentration(),
                    savedProduct.price(),
                    bottleResponse);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMessage().contains("bottles_barcode_key")) {
                throw new DuplicateBarcodeException("El código de barras ya está en uso. Usa un valor único.");
            }
            throw ex;
        }
    }

    @Override
    public List<ProductSummaryDTO> findAll(int page, int size) {
        return productRepositoryPort.findAll(page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailsResponse getProductDetails(Long id) {
        ProductEntity productEntity = productRepositoryPort.findById(id);
        List<Bottle> bottles = bottleRepositoryPort.findAllByProductId(id);
        List<BottleCreationResponse> bottleResponses = bottles.stream()
                .map(b -> new BottleCreationResponse(b.id(), b.barcode(), b.branchId(), b.volumeMl(),
                        b.remainingVolumeMl(), b.status()))
                .collect(Collectors.toList());
        return new ProductDetailsResponse(productEntity.getId(), productEntity.getBrand(), productEntity.getLine(),
                productEntity.getConcentration(), productEntity.getPrice(), productEntity.getUnitVolumeMl(),
                productEntity.getCreatedAt(), productEntity.getUpdatedAt(), bottleResponses);
    }
}
