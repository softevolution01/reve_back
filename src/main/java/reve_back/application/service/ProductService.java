package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.in.*;
import reve_back.application.ports.out.BottleRepositoryPort;
import reve_back.application.ports.out.ProductRepositoryPort;
import reve_back.domain.exception.DuplicateBarcodeException;
import reve_back.domain.model.Bottle;
import reve_back.domain.model.NewProduct;
import reve_back.domain.model.Product;
import reve_back.infrastructure.persistence.entity.ProductEntity;
import reve_back.infrastructure.web.dto.*;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ProductService implements ListProductsUseCase, CreateProductUseCase, GetProductDetailsUseCase, UpdateProductUseCase, DeleteProductUseCase {


    private final ProductRepositoryPort productRepositoryPort;
    private final BottleRepositoryPort bottleRepositoryPort;

    @Override
    @Transactional(rollbackFor = {DataIntegrityViolationException.class, Exception.class})
    public ProductCreationResponse createProduct(ProductCreationRequest request) {

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
        List<Bottle> savedBottles;
        try {
            savedBottles = bottleRepositoryPort.saveAll(bottles);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMessage().contains("bottles_barcode_key")) {
                throw new DuplicateBarcodeException("El código de barras ya está en uso. Usa un valor único.");
            }
            throw ex;
        }

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

    @Override
    @Transactional
    public ProductDetailsResponse updateProduct(Long id, ProductUpdateRequest request) {
        ProductEntity productEntity = productRepositoryPort.findById(id);
        productEntity.setBrand(request.brand());
        productEntity.setLine(request.line());
        productEntity.setConcentration(request.concentration());
        productEntity.setPrice(request.price());
        productEntity.setUnitVolumeMl(request.unitVolumeMl());
        ProductEntity updatedProduct = productRepositoryPort.update(productEntity);

        // Manejar botellas
        List<Bottle> existingBottles = bottleRepositoryPort.findAllByProductId(id);
        List<Bottle> updatedBottles = request.bottles().stream()
                .map(bottle -> {
                    Bottle existing = existingBottles.stream()
                            .filter(e -> e.barcode() != null && e.barcode().equals(bottle.barcode()))
                            .findFirst()
                            .orElse(null);
                    if (existing != null) {
                        return new Bottle(existing.id(), id, bottle.status(), bottle.barcode(),
                                bottle.volumeMl(), bottle.remainingVolumeMl(), bottle.branchId());
                    } else {
                        return new Bottle(null, id, bottle.status(), bottle.barcode(),
                                bottle.volumeMl(), bottle.remainingVolumeMl(), bottle.branchId());
                    }
                })
                .collect(Collectors.toList());

        try {
            List<Bottle> savedBottles = bottleRepositoryPort.updateAll(updatedBottles);
            List<BottleCreationResponse> bottleResponses = savedBottles.stream()
                    .map(b -> new BottleCreationResponse(
                            b.id(),
                            b.barcode(),
                            b.branchId(),
                            b.volumeMl(),
                            b.remainingVolumeMl(),
                            b.status()))
                    .collect(Collectors.toList());
            return new ProductDetailsResponse(updatedProduct.getId(), updatedProduct.getBrand(), updatedProduct.getLine(),
                    updatedProduct.getConcentration(), updatedProduct.getPrice(), updatedProduct.getUnitVolumeMl(),
                    updatedProduct.getCreatedAt(), updatedProduct.getUpdatedAt(), bottleResponses);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMessage().contains("bottles_barcode_key")) {
                throw new DuplicateBarcodeException("El código de barras ya está en uso. Usa un valor único.");
            }
            throw ex;
        }
    }

    @Override
    public void deleteProduct(Long id) {
        ProductEntity productEntity = productRepositoryPort.findById(id);
        List<Bottle> bottles = bottleRepositoryPort.findAllByProductId(id);
        if (!bottles.isEmpty()) {
            boolean allExhausted = bottles.stream().allMatch(b-> b.volumeMl() == 0 && b.remainingVolumeMl() == 0);
            if (!allExhausted) {
                throw new RuntimeException("No se puede eliminar: el producto tiene botellas asociadas no agotadas (volumen ml y volumen restante ml deben ser 0).");
            }
        }
        productEntity.set_active(false);
        productRepositoryPort.update(productEntity);
    }
}
