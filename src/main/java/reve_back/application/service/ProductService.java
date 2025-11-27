package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.in.*;
import reve_back.application.ports.out.BottleRepositoryPort;
import reve_back.application.ports.out.BranchRepositoryPort;
import reve_back.application.ports.out.DecantPriceRepositoryPort;
import reve_back.application.ports.out.ProductRepositoryPort;
import reve_back.domain.exception.DuplicateBarcodeException;
import reve_back.domain.model.*;
import reve_back.infrastructure.persistence.entity.DecantPriceEntity;
import reve_back.infrastructure.persistence.entity.ProductEntity;
import reve_back.infrastructure.util.BarcodeGenerator;
import reve_back.infrastructure.web.dto.*;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ProductService implements ListProductsUseCase, CreateProductUseCase, GetProductDetailsUseCase, UpdateProductUseCase, DeleteProductUseCase {


    private final ProductRepositoryPort productRepositoryPort;
    private final BottleRepositoryPort bottleRepositoryPort;
    private final BranchRepositoryPort branchRepositoryPort;
    private final DecantPriceRepositoryPort decantPriceRepositoryPort;

    @Override
    @Transactional(rollbackFor = {DataIntegrityViolationException.class, Exception.class})
    public ProductCreationResponse createProduct(ProductCreationRequest request) {

        // 1. Verificar si el producto ya existe (brand + line)
        if (productRepositoryPort.existsByBrandLineAndVolumeProductsMl(
                request.brand(),
                request.line(),
                request.unitVolumeMl() != null ? request.unitVolumeMl() : 0

        )) {
            throw new RuntimeException("El producto ya existe con esa marca y línea.");
        }

        // 2. Crear y guardar el producto
        NewProduct newProduct = new NewProduct(
                request.brand(),
                request.line(),
                request.concentration(),
                request.price(),
                request.unitVolumeMl()
        );
        Product savedProduct = productRepositoryPort.save(newProduct);

        List<Bottle> bottles;

        if (request.bottles() == null || request.bottles().isEmpty()) {
            // CASO 1: SIN BOTELLAS → AUTOMÁTICAS
            List<Branch> branches = branchRepositoryPort.findAll();
            if (branches.isEmpty()) {
                throw new RuntimeException("No hay sedes registradas en el sistema.");
            }

            bottles = branches.stream()
                    .map(branch -> new Bottle(
                            null,
                            savedProduct.id(),
                            BottlesStatus.AGOTADA.getValue(),          // estado por defecto
                            BarcodeGenerator.generateAlphanumeric(12), // barcode automático
                            0,                                       // volumeMl = 0
                            0,                                       // remainingVolumeMl = 0
                            0,
                            branch.id()                              // branchId de la sede
                    ))
                    .collect(Collectors.toList());

        }else {
            bottles = request.bottles().stream()
                    .map(req -> new Bottle(
                            null,
                            savedProduct.id(),
                            req.status(),
                            BarcodeGenerator.generateAlphanumeric(12),
                            req.volumeMl(),
                            req.remainingVolumeMl(),
                            req.quantity(),
                            req.branchId()
                    ))
                    .collect(Collectors.toList());
        }

        // 5. Guardar botellas
        List<Bottle> savedBottles;
        try {
            savedBottles = bottleRepositoryPort.saveAll(bottles);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("bottles_barcode_key")) {
                throw new RuntimeException("Error al generar códigos de barras únicos. Intenta de nuevo.");
            }
            throw ex;
        }

        // Mapear respuesta
        List<BottleCreationResponse> bottleResponse = savedBottles.stream()
                .map(b -> new BottleCreationResponse(
                        b.id(),
                        b.barcode(),
                        branchRepositoryPort.findAll().stream()
                                .filter(branch -> Objects.equals(branch.id(), b.branchId()))
                                .map(branch -> branch.name())
                                .findFirst()
                                .orElse("Sede no encontrada"),
                        b.volumeMl(),
                        b.remainingVolumeMl(),
                        b.quantity(),
                        b.status(),
                        BarcodeGenerator.generateBarcodeImageBase64(b.barcode())
                ))
                .collect(Collectors.toList());

        List<DecantResponse> decantResponses = List.of();
        if (request.decants() != null && !request.decants().isEmpty()) {
            List<DecantPrice> savedDecants = decantPriceRepositoryPort.saveAllForProduct(
                    savedProduct.id(),
                    request.decants()
            );
            decantResponses = savedDecants.stream()
                    .map(d -> new DecantResponse(
                            d.id(),
                            d.volumeMl(),
                            d.price(),
                            d.barcode(),
                            BarcodeGenerator.generateBarcodeImageBase64(d.barcode())))
                    .toList();
        }

        return new ProductCreationResponse(
                savedProduct.id(),
                savedProduct.brand(),
                savedProduct.line(),
                savedProduct.concentration(),
                savedProduct.price(),
                bottleResponse,
                decantResponses
        );

    }

    @Override
    public ProductPageResponse findAll(int page, int size) {
        Page<ProductSummaryDTO> productPage = productRepositoryPort.findAll(page, size);
        List<ProductListResponse> items = productPage.getContent().stream()
                .map(dto -> {
                    List<Bottle> bottles = bottleRepositoryPort.findAllByProductId(dto.id());
                    List<BottleCreationResponse> bottleResponses = bottles.stream()
                            .map(b -> new BottleCreationResponse(
                                    b.id(),
                                    b.barcode(),
                                    branchRepositoryPort.findAll().stream()
                                            .filter(branch -> Objects.equals(branch.id(), b.branchId()))
                                            .map(branch -> branch.name())
                                            .findFirst()
                                            .orElse("Sede no encontrada"),
                                    b.volumeMl(),
                                    b.remainingVolumeMl(),
                                    b.quantity(),
                                    b.status(),
                                    BarcodeGenerator.generateBarcodeImageBase64(b.barcode()) // GENERADO AQUÍ
                            ))
                            .collect(Collectors.toList());

                    List<DecantPriceEntity> decants = productRepositoryPort.findAllByProductId(dto.id());
                    List<DecantResponse> decantResponses = decants.stream()
                            .map(d -> new DecantResponse(
                                    d.getId(),
                                    d.getVolumeMl(),
                                    d.getPrice(),
                                    d.getBarcode(),
                                    BarcodeGenerator.generateBarcodeImageBase64(d.getBarcode())
                            ))
                            .toList();

                    return new ProductListResponse(
                            dto.id(),
                            dto.brand(),
                            dto.line(),
                            dto.concentration(),
                            dto.price(),
                            bottleResponses,
                            decantResponses
                    );
                })
                .collect(Collectors.toList());

        return new ProductPageResponse(
                items,
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.getNumber(),
                productPage.getSize()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailsResponse getProductDetails(Long id) {
        ProductEntity productEntity = productRepositoryPort.findById(id);
        if (!productEntity.is_active()) {
            throw new RuntimeException("Producto no encontrado o inactivo");
        }
        List<Bottle> bottles = bottleRepositoryPort.findAllByProductId(id);
        List<BottleCreationResponse> bottleResponses = bottles.stream()
                .map(b -> new BottleCreationResponse(b.id(), b.barcode(), branchRepositoryPort.findAll().stream()
                        .filter(branch -> Objects.equals(branch.id(), b.branchId()))
                        .map(branch -> branch.name())
                        .findFirst()
                        .orElse("Sede no encontrada"), b.volumeMl(),
                        b.remainingVolumeMl(),
                        b.quantity(),
                        b.status(),
                        BarcodeGenerator.generateBarcodeImageBase64(b.barcode())))
                .collect(Collectors.toList());

        List<DecantPriceEntity> decants = productRepositoryPort.findAllByProductId(id);
        List<DecantResponse> decantResponses = decants.stream()
                .map(d -> new DecantResponse(
                        d.getId(),
                        d.getVolumeMl(),
                        d.getPrice(),
                        d.getBarcode(),
                        BarcodeGenerator.generateBarcodeImageBase64(d.getBarcode())
                ))
                .toList();
        return new ProductDetailsResponse(productEntity.getId(), productEntity.getBrand(), productEntity.getLine(),
                productEntity.getConcentration(), productEntity.getPrice(),
                productEntity.getCreatedAt(), productEntity.getUpdatedAt(), bottleResponses, decantResponses);
    }

    @Override
    @Transactional
    public ProductDetailsResponse updateProduct(Long id, ProductUpdateRequest request) {
        ProductEntity productEntity = productRepositoryPort.findById(id);
        if (!productEntity.is_active()) {
            throw new RuntimeException("No se puede actualizar: el producto está inactivo o eliminado.");
        }

        List<Bottle> bottles = bottleRepositoryPort.findAllByProductId(id);
        boolean allAgotadas = bottles.stream()
                .allMatch(b -> "agotada".equalsIgnoreCase(b.status()));

        if (!allAgotadas) {
            throw new RuntimeException("No se puede editar el producto: tiene botellas activas o en otro estado.");
        }

        if (!productEntity.getBrand().equals(request.brand()) || !productEntity.getLine().equals(request.line())) {
            boolean exists = productRepositoryPort.existsByBrandLineAndVolumeProductsMlAndIdNot(
                    request.brand(),
                    request.line(),
                    request.unitVolumeMl() != null ? request.unitVolumeMl() : 0,
                    id
            );
            if (exists) {
                throw new DuplicateBarcodeException("Ya existe otro producto con esa marca y línea.");
            }
        }

        productEntity.setBrand(request.brand());
        productEntity.setLine(request.line());
        productEntity.setConcentration(request.concentration());
        productEntity.setPrice(request.price());
        ProductEntity updatedProduct = productRepositoryPort.update(productEntity);

        List<BottleCreationResponse> bottleResponses = bottles.stream()
                .map(b -> new BottleCreationResponse(
                        b.id(),
                        b.barcode(),
                        branchRepositoryPort.findAll().stream()
                                .filter(branch -> Objects.equals(branch.id(), b.branchId()))
                                .map(branch -> branch.name())
                                .findFirst()
                                .orElse("Sede no encontrada"),
                        b.volumeMl(),
                        b.remainingVolumeMl(),
                        b.quantity(),
                        b.status(),
                        BarcodeGenerator.generateBarcodeImageBase64(b.barcode())
                ))
                .collect(Collectors.toList());

        List<DecantPriceEntity> decantEntities = decantPriceRepositoryPort.findAllByProductId(id);
        List<DecantResponse> decantResponses = decantEntities.stream()
                .map(e -> new DecantResponse(
                        e.getId(),
                        e.getVolumeMl(),
                        e.getPrice(),
                        e.getBarcode(),
                        BarcodeGenerator.generateBarcodeImageBase64(e.getBarcode())
                ))
                .toList();

        return new ProductDetailsResponse(
                updatedProduct.getId(),
                updatedProduct.getBrand(),
                updatedProduct.getLine(),
                updatedProduct.getConcentration(),
                updatedProduct.getPrice(),
//                updatedProduct.getUnitVolumeMl(),
                updatedProduct.getCreatedAt(),
                updatedProduct.getUpdatedAt(),
                bottleResponses,
                decantResponses
        );
    }


    @Override
    public void deleteProduct(Long id) {
        ProductEntity productEntity = productRepositoryPort.findById(id);
        // verifica que el producto este activo
        if (!productEntity.is_active()) {
            throw new RuntimeException("No se puede eliminar: el producto ya está inactivo o no existe.");
        }
        List<Bottle> bottles = bottleRepositoryPort.findAllByProductId(id);
        if (!bottles.isEmpty()) {
            boolean allAgotadas = bottles.stream()
                    .allMatch(b -> "agotada".equalsIgnoreCase(b.status()));
            if (!allAgotadas) {
                throw new RuntimeException("No se puede eliminar: el producto tiene botellas no agotadas.");
            }
        }

        productEntity.set_active(false);
        productRepositoryPort.update(productEntity);
    }
}
