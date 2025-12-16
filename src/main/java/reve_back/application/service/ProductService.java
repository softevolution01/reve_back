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
public class ProductService implements ListProductsUseCase, CreateProductUseCase,
        GetProductDetailsUseCase, UpdateProductUseCase, DeleteProductUseCase, ScanBarcodeUseCase {


    private final ProductRepositoryPort productRepositoryPort;
    private final BottleRepositoryPort bottleRepositoryPort;
    private final BranchRepositoryPort branchRepositoryPort;
    private final DecantPriceRepositoryPort decantPriceRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final ProductDtoMapper mapper;

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

        // 2. Crear Producto (Usando Mapper para convertir Request -> Domain)
        NewProduct newProduct = mapper.toNewProductDomain(request);
        Product savedProduct = productRepositoryPort.save(newProduct);

        List<Bottle> bottles;

        if (request.bottles() == null || request.bottles().isEmpty()) {
            // Lógica: Crear botellas vacías para TODAS las sedes
            List<Branch> branches = branchRepositoryPort.findAll();
            if (branches.isEmpty()) {
                throw new RuntimeException("No hay sedes registradas en el sistema.");
            }
            bottles = branches.stream()
                    .map(branch -> new Bottle(
                            null,
                            savedProduct.id(),
                            BottlesStatus.AGOTADA.getValue(),
                            BarcodeGenerator.generateAlphanumeric(12),
                            0, 0, 0,
                            branch.id()
                    ))
                    .collect(Collectors.toList());
        } else {
            // Lógica: Crear botellas según lo que pide el usuario (Usando Mapper)
            bottles = request.bottles().stream()
                    .map(req -> mapper.toBottleDomain(req, savedProduct.id()))
                    .collect(Collectors.toList());
        }

        // 3. Guardar Botellas
        List<Bottle> savedBottles;
        try {
            savedBottles = bottleRepositoryPort.saveAll(bottles);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("bottles_barcode_key")) {
                throw new RuntimeException("Error al generar códigos de barras únicos. Intenta de nuevo.");
            }
            throw ex;
        }

        // 4. Guardar Decants (si existen)
        List<DecantPrice> savedDecants = List.of();
        if (request.decants() != null && !request.decants().isEmpty()) {
            savedDecants = decantPriceRepositoryPort.saveAllForProduct(
                    savedProduct.id(),
                    request.decants()
            );
        }

        // 5. Construir Respuesta Final (Usando Mapper para todo)
        List<BottleCreationResponse> bottleResponses = savedBottles.stream()
                .map(mapper::toBottleResponse)
                .toList();

        List<DecantResponse> decantResponses = savedDecants.stream()
                .map(mapper::toDecantResponse)
                .toList();

        return mapper.toProductCreationResponse(savedProduct, bottleResponses, decantResponses);
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
                            .map(e -> new DecantPrice(e.getId(), e.getProductId(), e.getVolumeMl(), e.getPrice(), e.getBarcode()))
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
                .map(e -> new DecantPrice(e.getId(), e.getProductId(), e.getVolumeMl(), e.getPrice(), e.getBarcode()))
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
        // 1. Validaciones iniciales
        ProductEntity productEntity = productRepositoryPort.findById(id);
        if (!productEntity.is_active()) {
            throw new RuntimeException("Producto inactivo o eliminado.");
        }

        List<Bottle> allBottles = bottleRepositoryPort.findAllByProductId(id);
        if (allBottles.stream().anyMatch(b -> !"agotada".equalsIgnoreCase(b.status()))) {
            throw new RuntimeException("No puedes editar: hay botellas selladas o con stock en alguna sede.");
        }

        // 2. Validación de duplicados
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

        // 3. Actualizar entidad Producto
        productEntity.setBrand(request.brand());
        productEntity.setLine(request.line());
        productEntity.setConcentration(request.concentration());
        productEntity.setPrice(request.price());
        productEntity.setVolumeProductsMl(request.unitVolumeMl());
        productRepositoryPort.update(productEntity);

        // 4. Lógica de mezcla (Merge) de Botellas
        Map<Long, Bottle> existingMap = allBottles.stream()
                .collect(Collectors.toMap(Bottle::branchId, b -> b));

        List<Bottle> toSave = new ArrayList<>();

        for (BottleCreationRequest req : request.bottles()) {
            if (req.branchId() == null) {
                throw new RuntimeException("branchId es obligatorio");
            }

            Bottle existing = existingMap.get(req.branchId());

            if (existing != null) {
                // Actualizar existente
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
                // Crear nueva (Usamos el mapper para generar el barcode y estructura base)
                // Nota: Mantenemos la lógica de "agotada" por defecto si viene null
                Bottle newBottleBase = mapper.toBottleDomain(req, id);

                // Si el status venía null, el mapper lo deja null, aplicamos la regla de negocio aquí:
                String finalStatus = req.status() != null ? req.status() : "agotada";

                // Reconstruimos con el status asegurado (los records son inmutables)
                toSave.add(new Bottle(
                        null,
                        id,
                        finalStatus,
                        newBottleBase.barcode(),
                        newBottleBase.volumeMl() != null ? newBottleBase.volumeMl() : 100,
                        newBottleBase.remainingVolumeMl() != null ? newBottleBase.remainingVolumeMl() : 100,
                        newBottleBase.quantity() != null ? newBottleBase.quantity() : 1,
                        req.branchId()
                ));
            }
        }

        bottleRepositoryPort.updateAll(toSave);

        // 5. Preparar Respuesta (¡Aquí es donde brilla el Mapper!)
        List<BottleCreationResponse> bottleResp = bottleRepositoryPort.findAllByProductId(id).stream()
                .map(mapper::toBottleResponse)
                .toList();

        List<DecantResponse> decants = decantPriceRepositoryPort.findAllByProductId(id).stream()
                .map(e -> new DecantPrice(e.getId(), e.getProductId() ,e.getVolumeMl(), e.getPrice(), e.getBarcode()))
                .map(mapper::toDecantResponse)
                .toList();

        return mapper.toProductDetailsResponse(productEntity, bottleResp, decants);
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

    @Override
    public ScanBarcodeResponse scanBarcode(String barcode) {
        // REGLA 1: Buscar en bottles.barcode
        // Solo botellas con status = 'sellada'
        Optional<Bottle> bottleOpt = bottleRepositoryPort.findByBarcodeAndStatus(barcode, "sellada");

        if (bottleOpt.isPresent()) {
            Bottle bottle = bottleOpt.get();

            // Obtenemos info del producto padre (Marca, Línea)
            ProductEntity productEntity = productRepositoryPort.findById(bottle.productId());

            // Creamos objeto temporal de dominio para el mapper
            Product product = new Product(
                    productEntity.getId(),
                    productEntity.getBrand(),
                    productEntity.getLine(),
                    productEntity.getConcentration(),
                    productEntity.getPrice()
            );

            // "La venta se realiza contra ese bottles.id" -> idInventario = bottle.id
            return mapper.toScanResponse(bottle, product, product.price());
        }

        // REGLA 2: Si no es botella, buscar en decant_prices.barcode
        Optional<DecantPrice> decantOpt = decantPriceRepositoryPort.findByBarcode(barcode);

        if (decantOpt.isPresent()) {
            DecantPrice decant = decantOpt.get();

            ProductEntity productEntity = productRepositoryPort.findById(decant.productId());

            Product product = new Product(
                    productEntity.getId(),
                    productEntity.getBrand(),
                    productEntity.getLine(),
                    productEntity.getConcentration(),
                    productEntity.getPrice()
            );

            // "La venta se realiza contra el decant_prices.id" -> idInventario = decant.id
            return mapper.toScanResponse(decant, product);
        }

        throw new RuntimeException("Producto no encontrado con código de barras: " + barcode);
    }
}
