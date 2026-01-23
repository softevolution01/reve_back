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
import reve_back.infrastructure.mapper.DecantDtoMapper;
import reve_back.infrastructure.mapper.ProductDtoMapper;
import reve_back.infrastructure.util.BarcodeGenerator;
import reve_back.infrastructure.web.dto.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ProductService implements ListProductsUseCase, CreateProductUseCase,
        GetProductDetailsUseCase, UpdateProductUseCase, DeleteProductUseCase, ScanBarcodeUseCase, SearchProductsUseCase, GetLabelCatalogUseCase {


    private final ProductRepositoryPort productRepositoryPort;
    private final BottleRepositoryPort bottleRepositoryPort;
    private final DecantPriceRepositoryPort decantPriceRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final ProductDtoMapper productDtoMapper;
    private final DecantDtoMapper decantDtoMapper;

    @Override
    @Transactional(rollbackFor = {DataIntegrityViolationException.class, Exception.class})
    public ProductCreationResponse createProduct(ProductCreationRequest request) {

        if (productRepositoryPort.existsByBrandAndLineAndConcentrationAndVolumeProductsMl(
                request.brand(),
                request.line(),
                request.concentration(),
                request.unitVolumeMl() != null ? request.unitVolumeMl() : 0
        )) {
            throw new RuntimeException("El producto ya existe con esa marca, línea o volumen.");
        }

        Product product = productDtoMapper.toDomain(request);
        Product savedProduct = productRepositoryPort.save(product);

        List<Bottle> bottlesToSave = new ArrayList<>();

        if (request.bottles() == null || request.bottles().isEmpty()) {

            List<Warehouse> warehouses = warehouseRepositoryPort.findAll();
            if (warehouses.isEmpty()) {
                throw new RuntimeException("No hay almacenes registrados (necesarios para el stock).");
            }
            for (Warehouse warehouse : warehouses) {
                bottlesToSave.add(new Bottle(
                        null,
                        savedProduct.id(),
                        warehouse.id(),
                        BottlesStatus.AGOTADA.getValue(),
                        BarcodeGenerator.generateAlphanumeric(12),
                        0,
                        0,
                        0

                ));

                bottlesToSave.add(new Bottle(
                        null,
                        savedProduct.id(),
                        warehouse.id(),
                        BottlesStatus.DECANT_AGOTADA.getValue(),
                        null,
                        0,
                        0,
                        0
                ));
            }
        } else {
            bottlesToSave = request.bottles().stream()
                    .map(req -> productDtoMapper.toBottleDomain(req, savedProduct.id()))
                    .collect(Collectors.toList());
        }

        List<Bottle> savedBottles;

        try {
            savedBottles = bottleRepositoryPort.saveAll(bottlesToSave);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("bottles_barcode_key")) {
                throw new RuntimeException("Error al generar códigos de barras únicos. Intenta de nuevo.");
            }
            throw ex;
        }

        // 4. Guardar Decants (si existen)
        List<DecantPrice> savedDecants = List.of();

        if (request.decants() != null && !request.decants().isEmpty()) {
            // CONVERSIÓN: De Request (DTO) a DecantPrice (Dominio)
            List<DecantPrice> decantsToSave = request.decants().stream()
                    .map(decantDtoMapper::toDomain)
                    .toList();

            // Ahora enviamos la lista correcta al puerto
            savedDecants = decantPriceRepositoryPort.saveAllForProduct(
                    savedProduct.id(),
                    decantsToSave
            );
        }

        // 5. Construir Respuesta Final (Usando Mapper para todo)
        List<BottleCreationResponse> bottleResponses = savedBottles.stream()
                .map(productDtoMapper::toBottleResponse)
                .toList();

        List<DecantResponse> decantResponses = savedDecants.stream()
                .map(decantDtoMapper::toResponse)
                .toList();

        return productDtoMapper.toProductCreationResponse(savedProduct, bottleResponses, decantResponses);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductPageResponse findAll(int page, int size, String query) {
        // 1. Obtener IDs de sedes autorizadas para el usuario actual
        Set<Long> userBranchIds = getAuthorizedBranchIds();

        // 2. Obtener la página de productos desde el puerto
        Page<ProductSummaryDTO> productPage;

        if (query != null && !query.trim().isEmpty()) {
            // SI HAY QUERY: Buscamos por marca o línea
            productPage = productRepositoryPort.searchByBrandOrLine(query.trim(), page, size);
        } else {
            // NO HAY QUERY: Traemos todo (Tu lógica original)
            productPage = productRepositoryPort.findAll(page, size);
        }

        // 3. Procesar cada producto para incluir sus botellas y decants
        List<ProductListResponse> items = productPage.getContent().stream()
                .map(summary -> {
                    // Buscamos todas las botellas del producto
                    List<Bottle> allBottles = bottleRepositoryPort.findAllByProductId(summary.id());

                    // Filtramos botellas: Solo las que pertenecen a las sedes del usuario
                    List<BottleCreationResponse> validBottles = allBottles.stream()
                            .filter(b -> userBranchIds.contains(b.warehouseId()))
                            .map(productDtoMapper::toBottleResponse)
                            .toList();

                    // REGLA DE NEGOCIO: Si el usuario no tiene stock/botellas en sus sedes,
                    // no mostramos este producto en su lista.
                    if (validBottles.isEmpty()) {
                        return null;
                    }

                    // Buscamos los precios de decants
                    List<DecantResponse> validDecants = decantPriceRepositoryPort.findAllByProductId(summary.id())
                            .stream()
                            .map(decantDtoMapper::toResponse)
                            .toList();

                    // Mapeamos a la respuesta final de la lista
                    return productDtoMapper.toProductListResponse(summary, validBottles, validDecants);
                })
                .filter(Objects::nonNull) // Eliminamos los productos que el usuario no puede ver
                .toList();

        // 4. Devolver respuesta paginada para el Frontend
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

        Product product = productRepositoryPort.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (!product.isActive()) throw new RuntimeException("Producto inactivo");

        List<BottleCreationResponse> bottleResponses = bottleRepositoryPort.findAllByProductId(id).stream()
                .filter(b -> userBranchIds.contains(b.warehouseId()))
                .map(productDtoMapper::toBottleResponse)
                .toList();

        if (bottleResponses.isEmpty()) throw new RuntimeException("No tienes acceso a este producto en tus sedes autorizadas");

        List<DecantResponse> decantResponses = decantPriceRepositoryPort.findAllByProductId(id).stream()
                .map(decantDtoMapper::toResponse)
                .toList();

        return productDtoMapper.toProductDetailsResponse(product, bottleResponses, decantResponses);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDetailsResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product existingProduct = productRepositoryPort.findById(id)
                .orElseThrow(() -> new RuntimeException("No existe el producto a actualizar"));

        String brand = request.brand().toUpperCase().trim();
        String line = request.line().toUpperCase().trim();
        String concentration = request.concentration().toUpperCase().trim();

        List<Bottle> allBottles = bottleRepositoryPort.findAllByProductId(id);

        boolean hasPhysicalStock = allBottles.stream()
                .anyMatch(b -> !BottlesStatus.AGOTADA.name().equalsIgnoreCase(b.status())
                        && !BottlesStatus.DECANT_AGOTADA.name().equalsIgnoreCase(b.status()));

        if (hasPhysicalStock) {
            throw new RuntimeException("No puedes editar propiedades críticas: hay stock físico involucrado.");
        }

        if (productRepositoryPort.existsByBrandAndLineAndConcentrationAndVolumeProductsMlAndIdNot(
                brand, line, concentration, request.unitVolumeMl(), id)) {
            throw new RuntimeException("Ya existe otro producto con los mismos datos: " + brand + " " + line);
        }

        Product productToUpdate = productDtoMapper.toDomain(id, request, existingProduct);
        productRepositoryPort.save(productToUpdate);

        Map<Long, Bottle> existingBottlesMap = allBottles.stream()
                .collect(Collectors.toMap(Bottle::warehouseId, b -> b, (e1, e2) -> e1));

        List<Bottle> bottlesToSync = new ArrayList<>();
        if (request.bottles() != null) {
            for (BottleCreationRequest req : request.bottles()) {
                Bottle existing = existingBottlesMap.get(req.warehouseId());
                if (existing != null) {
                    bottlesToSync.add(new Bottle(existing.id(), id, req.warehouseId(),
                            req.status() != null ? req.status() : existing.status(),
                            existing.barcode(),
                            req.volumeMl() != null ? req.volumeMl() : existing.volumeMl(),
                            req.remainingVolumeMl() != null ? req.remainingVolumeMl() : existing.remainingVolumeMl(),
                            req.quantity() != null ? req.quantity() : existing.quantity()));
                } else {
                    bottlesToSync.add(productDtoMapper.toBottleDomain(req, id));
                }
            }
        }

        if (!bottlesToSync.isEmpty()) bottleRepositoryPort.saveAll(bottlesToSync);

        if (request.decants() != null) {
            List<DecantPrice> currentDecants = decantPriceRepositoryPort.findAllByProductId(id);

            Map<Integer, DecantPrice> existingDecantsMap = currentDecants.stream()
                    .collect(Collectors.toMap(DecantPrice::volumeMl, d -> d));

            List<DecantPrice> decantsToSave = new ArrayList<>();

            for (DecantRequest decReq : request.decants()) {
                DecantPrice existingDecant = existingDecantsMap.get(decReq.volumeMl());

                if (existingDecant != null) {
                    decantsToSave.add(new DecantPrice(
                            existingDecant.id(),
                            id,
                            decReq.volumeMl(),
                            decReq.price(),
                            existingDecant.barcode(),
                            existingDecant.imageBarcode()
                    ));
                } else {
                    decantsToSave.add(new DecantPrice(
                            null,
                            id,
                            decReq.volumeMl(),
                            decReq.price(),
                            BarcodeGenerator.generateAlphanumeric(12),
                            null
                    ));
                }
            }

            // C. Guardar usando el método específico de tu puerto
            if (!decantsToSave.isEmpty()) {
                decantPriceRepositoryPort.saveAllForProduct(id, decantsToSave);
            }
        }
        // =================================================================================

        return getProductDetails(id);
    }


    @Override
    public void deleteProduct(Long id) {
        Product product = productRepositoryPort.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (!product.isActive()) throw new RuntimeException("El producto ya está inactivo.");

        List<Bottle> bottles = bottleRepositoryPort.findAllByProductId(id);
        boolean canDelete = bottles.stream().allMatch(b -> "AGOTADA".equalsIgnoreCase(b.status()) || "DECANT_AGOTADA".equalsIgnoreCase(b.status()));

        if (!canDelete) throw new RuntimeException("No se puede eliminar: el producto tiene botellas asociadas con stock.");

        productRepositoryPort.setInactiveById(id);
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
        var decantOpt = decantPriceRepositoryPort.findByBarcode(barcode);

        if (decantOpt.isPresent()) {
            var decant = decantOpt.get();
            var product = getProductOrThrow(decant.productId());

            Integer totalStockMl = bottleRepositoryPort.calculateTotalStockByProductId(product.id());

            return productDtoMapper.toScanResponse(decant, product, totalStockMl);
        }
        var bottleOpt = bottleRepositoryPort.findByBarcodeAndStatus(barcode, BottlesStatus.SELLADA);

        if (bottleOpt.isPresent()) {
            var bottle = bottleOpt.get();
            var product = getProductOrThrow(bottle.productId());

            Integer totalStockMl = bottleRepositoryPort.calculateTotalStockByProductId(product.id());

            return productDtoMapper.toScanResponse(bottle, product, product.price().doubleValue(),totalStockMl);
        }

        throw new RuntimeException("Código de barras no encontrado: " + barcode);
    }

    private Product getProductOrThrow(Long productId) {
        return productRepositoryPort.findById(productId)
                .orElseThrow(() -> new RuntimeException("Inconsistencia: Producto no encontrado para ID " + productId));
    }

    @Override
    public List<ProductSearchResponse> searchProducts(String term) {
        List<ProductSearchResponse> results = new ArrayList<>();

        List<Bottle> bottles = bottleRepositoryPort.searchActiveByProductName(term);
        for (Bottle b : bottles) {
            var p = productRepositoryPort.findById(b.productId()).orElseThrow();
            Integer totalStock = bottleRepositoryPort.calculateTotalStockByProductId(p.id());
            results.add(productDtoMapper.toSearchResponse(b, p,totalStock));
        }

        List<DecantPrice> decants = decantPriceRepositoryPort.searchActiveByProductName(term);
        for (DecantPrice d : decants) {
            var p = productRepositoryPort.findById(d.productId()).orElseThrow();
            Integer totalStock = bottleRepositoryPort.calculateTotalStockByProductId(p.id());
            results.add(productDtoMapper.toSearchResponse(d, p,totalStock));
        }

        return results;
    }

    @Override
    public List<LabelItemDTO> execute() {
        return productRepositoryPort.getLabelCatalog();
    }
}
