package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.in.*;
import reve_back.application.ports.out.*;
import reve_back.domain.model.*;
import reve_back.infrastructure.mapper.ProductDtoMapper;
import reve_back.infrastructure.persistence.entity.ProductEntity;
import reve_back.infrastructure.util.BarcodeGenerator;
import reve_back.infrastructure.web.dto.*;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ProductService implements ListProductsUseCase, CreateProductUseCase, GetProductDetailsUseCase, UpdateProductUseCase, DeleteProductUseCase {


    private final ProductRepositoryPort productRepositoryPort;
    private final BottleRepositoryPort bottleRepositoryPort;
    private final BranchRepositoryPort branchRepositoryPort;
    private final DecantPriceRepositoryPort decantPriceRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final ProductDtoMapper mapper;
//    private final UserJpaRepository userJpaRepository;

    @Override
    @Transactional(rollbackFor = {DataIntegrityViolationException.class, Exception.class})
    public ProductCreationResponse createProduct(ProductCreationRequest request) {

        if (productRepositoryPort.existsByBrandLineAndVolumeProductsMl(
                request.brand(),
                request.line(),
                request.unitVolumeMl() != null ? request.unitVolumeMl() : 0

        )) {
            throw new RuntimeException("El producto ya existe con esa marca, línea o volumen.");
        }

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
        Set<Long> userBranchIds = getAuthorizedBranchIds();
        Page<ProductSummaryDTO> productPage = productRepositoryPort.findAll(page, size);
        List<ProductListResponse> items = productPage.getContent().stream()
                .map(summary -> {
                    // 1. Obtener datos
                    List<Bottle> allBottles = bottleRepositoryPort.findAllByProductId(summary.id());
                    List<DecantPrice> allDecants = decantPriceRepositoryPort.findAllByProductId(summary.id())
                            .stream()
                            .map(e -> new DecantPrice(e.getId(), e.getVolumeMl(), e.getPrice(), e.getBarcode()))
                            .toList();

                    // 2. Filtrar botellas por sede del usuario
                    List<BottleCreationResponse> validBottles = allBottles.stream()
                            .filter(b -> userBranchIds.contains(b.branchId()))
                            .map(mapper::toBottleResponse)
                            .toList();

                    // Si no tiene botellas en sus sedes, no mostramos el producto
                    if (validBottles.isEmpty()) {
                        return null;
                    }

                    // 3. Mapear decants
                    List<DecantResponse> validDecants = allDecants.stream()
                            .map(mapper::toDecantResponse)
                            .toList();

                    // 4. Retornar respuesta final
                    return mapper.toProductListResponse(summary, validBottles, validDecants);
                })
                .filter(Objects::nonNull)
                .toList();

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
        Set<Long> userBranchIds = getAuthorizedBranchIds();

        ProductEntity product = productRepositoryPort.findById(id);
        if (!product.is_active()) throw new RuntimeException("Producto inactivo");

        // Usamos el mapper
        List<BottleCreationResponse> bottleResponses = bottleRepositoryPort.findAllByProductId(id).stream()
                .filter(b -> userBranchIds.contains(b.branchId()))
                .map(mapper::toBottleResponse)
                .toList();

        if (bottleResponses.isEmpty()) throw new RuntimeException("No tienes acceso");

        // Usamos el mapper
        List<DecantResponse> decantResponses = decantPriceRepositoryPort.findAllByProductId(id).stream()
                .map(e -> new DecantPrice(e.getId(), e.getVolumeMl(), e.getPrice(), e.getBarcode())) // A dominio
                .map(mapper::toDecantResponse)
                .toList();

        return new ProductDetailsResponse(
                product.getId(),
                product.getBrand(),
                product.getLine(),
                product.getConcentration(),
                product.getPrice(),
                product.getVolumeProductsMl(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                bottleResponses,
                decantResponses
        );
    }

    @Override
    @Transactional
    public ProductDetailsResponse updateProduct(Long id, ProductUpdateRequest request) {
        ProductEntity productEntity = productRepositoryPort.findById(id);
        if (!productEntity.is_active()) {
            throw new RuntimeException("Producto inactivo o eliminado.");
        }

        List<Bottle> allBottles = bottleRepositoryPort.findAllByProductId(id);
        if (allBottles.stream().anyMatch(b -> !"agotada".equalsIgnoreCase(b.status()))) {
            throw new RuntimeException("No puedes editar: hay botellas selladas o con stock en alguna sede.");
        }

        if (!productEntity.getBrand().equals(request.brand()) ||
                !productEntity.getLine().equals(request.line()) ||
                !Objects.equals(productEntity.getVolumeProductsMl(), request.unitVolumeMl())) {

            boolean exists = productRepositoryPort.existsByBrandLineAndVolumeProductsMlAndIdNot(
                    request.brand(), request.line(),
                    request.unitVolumeMl() != null ? request.unitVolumeMl() : 0, id
            );
            if (exists) {
                throw new RuntimeException("Ya existe otro producto con esa marca, línea y volumen.");
            }
        }

        productEntity.setBrand(request.brand());
        productEntity.setLine(request.line());
        productEntity.setConcentration(request.concentration());
        productEntity.setPrice(request.price());
        productEntity.setVolumeProductsMl(request.unitVolumeMl());
        productRepositoryPort.update(productEntity);

        Map<Long, Bottle> existingMap = allBottles.stream()
                .collect(Collectors.toMap(Bottle::branchId, b -> b));

        List<Bottle> toSave = new ArrayList<>();

        for (BottleCreationRequest req : request.bottles()) {
            System.out.println("Request quantity: " + req.quantity());
            if (req.branchId() == null) {
                throw new RuntimeException("branchId es obligatorio");
            }

            Bottle existing = existingMap.get(req.branchId());

            if (existing != null) {
                toSave.add(new Bottle(
                        existing.id(),
                        id,
                        req.status() != null ? req.status() : existing.status(),
                        existing.barcode(),
                        req.volumeMl() != null ? req.volumeMl() : existing.volumeMl(),
                        req.remainingVolumeMl() != null ? req.remainingVolumeMl() : existing.remainingVolumeMl(),
                        req.quantity() != null ? req.quantity() : existing.quantity(),
                        req.branchId()
                ));
            } else {
                toSave.add(new Bottle(
                        null,
                        id,
                        req.status() != null ? req.status() : "agotada",
                        BarcodeGenerator.generateAlphanumeric(12),
                        req.volumeMl() != null ? req.volumeMl() : 100,
                        req.remainingVolumeMl() != null ? req.remainingVolumeMl() : 100,
                        req.quantity() != null ? req.quantity() : 1,
                        req.branchId()
                ));
            }
        }

        bottleRepositoryPort.updateAll(toSave);

        List<Bottle> updatedBottles = bottleRepositoryPort.findAllByProductId(id);

        List<BottleCreationResponse> bottleResp = updatedBottles.stream()
                .map(b -> new BottleCreationResponse(
                        b.id(),
                        b.barcode(),
                        branchRepositoryPort.findAll().stream()
                                .filter(br -> br.id().equals(b.branchId()))
                                .map(Branch::name)
                                .findFirst().orElse("Sede desconocida"),
                        b.volumeMl(),
                        b.remainingVolumeMl(),
                        b.quantity(),
                        b.status(),
                        BarcodeGenerator.generateBarcodeImageBase64(b.barcode())
                ))
                .toList();

        List<DecantResponse> decants = decantPriceRepositoryPort.findAllByProductId(id).stream()
                .map(d -> new DecantResponse(d.getId(), d.getVolumeMl(), d.getPrice(), d.getBarcode(),
                        BarcodeGenerator.generateBarcodeImageBase64(d.getBarcode())))
                .toList();

        return new ProductDetailsResponse(
                productEntity.getId(),
                productEntity.getBrand(),
                productEntity.getLine(),
                productEntity.getConcentration(),
                productEntity.getPrice(),
                productEntity.getVolumeProductsMl(),
                productEntity.getCreatedAt(),
                productEntity.getUpdatedAt(),
                bottleResp,
                decants
        );
    }


    @Override
    public void deleteProduct(Long id) {
        ProductEntity productEntity = productRepositoryPort.findById(id);
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

    private Set<Long> getAuthorizedBranchIds() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepositoryPort.findByUsername(username)
                .map(user -> user.branches().stream()
                        .map(Branch::id)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }
}
