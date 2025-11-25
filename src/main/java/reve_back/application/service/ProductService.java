package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.in.*;
import reve_back.application.ports.out.BottleRepositoryPort;
import reve_back.application.ports.out.BranchRepositoryPort;
import reve_back.application.ports.out.ProductRepositoryPort;
import reve_back.domain.exception.DuplicateBarcodeException;
import reve_back.domain.model.*;
import reve_back.infrastructure.persistence.entity.ProductEntity;
import reve_back.infrastructure.util.BarcodeGenerator;
import reve_back.infrastructure.web.dto.*;

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ProductService implements ListProductsUseCase, CreateProductUseCase, GetProductDetailsUseCase, UpdateProductUseCase, DeleteProductUseCase {


    private final ProductRepositoryPort productRepositoryPort;
    private final BottleRepositoryPort bottleRepositoryPort;
    private final BranchRepositoryPort branchRepositoryPort;

    @Override
    @Transactional(rollbackFor = {DataIntegrityViolationException.class, Exception.class})
    public ProductCreationResponse createProduct(ProductCreationRequest request) {

        // 1. Verificar si el producto ya existe (brand + line)
        if (productRepositoryPort.existsByBrandAndLine(request.brand(), request.line())) {
            throw new RuntimeException("El producto ya existe con esa marca y línea.");
        }

        // 2. Crear y guardar el producto
        NewProduct newProduct = new NewProduct(
                request.brand(),
                request.line(),
                request.concentration(),
                request.price()
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
                            generateBarcode(12),               // barcode automático
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
                            generateBarcode(12),
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
                        b.branchId(),
                        b.volumeMl(),
                        b.remainingVolumeMl(),
                        b.quantity(),
                        b.status(),
                        BarcodeGenerator.generateBarcodeImageBase64(b.barcode())))
                .collect(Collectors.toList());

        return new ProductCreationResponse(
                savedProduct.id(),
                savedProduct.brand(),
                savedProduct.line(),
                savedProduct.concentration(),
                savedProduct.price(),
                bottleResponse);

    }

    private String generateBarcode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
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
                                    b.branchId(),
                                    b.volumeMl(),
                                    b.remainingVolumeMl(),
                                    b.quantity(),
                                    b.status(),
                                    BarcodeGenerator.generateBarcodeImageBase64(b.barcode()) // GENERADO AQUÍ
                            ))
                            .collect(Collectors.toList());

                    return new ProductListResponse(
                            dto.id(),
                            dto.brand(),
                            dto.line(),
                            dto.concentration(),
                            dto.price(),
                            bottleResponses
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
                .map(b -> new BottleCreationResponse(b.id(), b.barcode(), b.branchId(), b.volumeMl(),
                        b.remainingVolumeMl(),
                        b.quantity(),
                        b.status(),
                        BarcodeGenerator.generateBarcodeImageBase64(b.barcode())))
                .collect(Collectors.toList());
        return new ProductDetailsResponse(productEntity.getId(), productEntity.getBrand(), productEntity.getLine(),
                productEntity.getConcentration(), productEntity.getPrice(),
                productEntity.getCreatedAt(), productEntity.getUpdatedAt(), bottleResponses);
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
            boolean exists = productRepositoryPort.existsByBrandAndLineAndIdNot(request.brand(), request.line(), id);
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
                        b.branchId(),
                        b.volumeMl(),
                        b.remainingVolumeMl(),
                        b.quantity(),
                        b.status(),
                        BarcodeGenerator.generateBarcodeImageBase64(b.barcode())
                ))
                .collect(Collectors.toList());

        return new ProductDetailsResponse(
                updatedProduct.getId(),
                updatedProduct.getBrand(),
                updatedProduct.getLine(),
                updatedProduct.getConcentration(),
                updatedProduct.getPrice(),
                updatedProduct.getCreatedAt(),
                updatedProduct.getUpdatedAt(),
                bottleResponses
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
