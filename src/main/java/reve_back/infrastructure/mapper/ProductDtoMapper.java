package reve_back.infrastructure.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reve_back.application.ports.out.WarehouseRepositoryPort;
import reve_back.domain.model.*;
import reve_back.infrastructure.util.BarcodeGenerator;
import reve_back.infrastructure.web.dto.*;

import java.util.List;

@RequiredArgsConstructor
@Component
public class ProductDtoMapper {

    private final WarehouseRepositoryPort warehouseRepositoryPort;

    public Product toDomain(ProductCreationRequest request) {
        return new Product(
                null,
                request.brand().toUpperCase().trim(),
                request.line().toUpperCase().trim(),
                request.concentration().toUpperCase().trim(),
                request.price(),
                request.unitVolumeMl(),
                true,
                true,
                null,
                null
        );
    }

    public Product toDomain(Long id, ProductUpdateRequest request, Product existing) {
        return new Product(
                id,
                request.brand().toUpperCase().trim(),
                request.line().toUpperCase().trim(),
                request.concentration().toUpperCase().trim(),
                request.price(),
                request.unitVolumeMl(),
                existing.isActive(),
                existing.allowPromotions(),
                existing.createdAt(),
                null
        );
    }

    public BottleCreationResponse toBottleResponse(Bottle b) {
        // En lugar de buscar en todas las sedes, buscamos el nombre del Almacén
        String warehouseName = warehouseRepositoryPort.findById(b.warehouseId())
                .map(Warehouse::name)
                .orElse("Almacén no encontrado");

        return new BottleCreationResponse(
                b.id(),
                b.barcode(),
                warehouseName,
                b.volumeMl(),
                b.remainingVolumeMl(),
                b.quantity(),
                b.status(),
                b.barcode() != null ? BarcodeGenerator.generateBarcodeImageBase64(b.barcode()) : null
        );
    }

    public DecantResponse toDecantResponse(DecantPrice d) {
        return new DecantResponse(
                d.id(),
                d.volumeMl(),
                d.price(),
                d.barcode(),
                BarcodeGenerator.generateBarcodeImageBase64(d.barcode())
        );
    }

    public ProductListResponse toProductListResponse(ProductSummaryDTO dto, List<BottleCreationResponse> bottles, List<DecantResponse> decants) {
        return new ProductListResponse(
                dto.id(),
                dto.brand(),
                dto.line(),
                dto.concentration(),
                dto.price(),
                dto.volumeProductsMl(),
                bottles,
                decants
        );
    }

    public ProductCreationResponse toProductCreationResponse(Product p, List<BottleCreationResponse> bottles, List<DecantResponse> decants) {
        return new ProductCreationResponse(
                p.id(),
                p.brand(),
                p.line(),
                p.concentration(),
                p.price(),
                bottles,
                decants
        );
    }

    public Bottle toBottleDomain(BottleCreationRequest req, Long productId) {
        return new Bottle(
                null,
                productId,
                req.warehouseId(),
                req.status(),
                BarcodeGenerator.generateAlphanumeric(12),
                req.volumeMl(),
                req.remainingVolumeMl(),
                req.quantity()
        );
    }

    public ProductDetailsResponse toProductDetailsResponse(Product p, List<BottleCreationResponse> bottles, List<DecantResponse> decants) {
        return new ProductDetailsResponse(
                p.id(),
                p.brand(),
                p.line(),
                p.concentration(),
                p.price(),
                p.volumeProductsMl(),
                p.createdAt(),
                p.updatedAt(),
                bottles,
                decants
        );
    }

    public ScanBarcodeResponse toScanResponse(Bottle bottle, Product product, Double priceOverride) {
        return new ScanBarcodeResponse(
                bottle.id(),
                "BOTELLA",
                product.id(),
                product.brand(),
                product.line(),
                product.concentration(),
                bottle.volumeMl(),
                priceOverride,
                product.allowPromotions()
        );
    }

    public ScanBarcodeResponse toScanResponse(DecantPrice decant, Product product) {
        return new ScanBarcodeResponse(
                decant.id(),
                "DECANT",
                product.id(),
                product.brand(),
                product.line(),
                product.concentration(),
                decant.volumeMl(),
                decant.price(),
                product.allowPromotions()
        );
    }

    public ProductSearchResponse toSearchResponse(Bottle bottle, Product product) {
        return new ProductSearchResponse(
                bottle.id(),
                "BOTELLA",
                product.id(),
                product.brand(),
                product.line(),
                bottle.volumeMl(),
                product.price().doubleValue(),
                product.allowPromotions()
        );
    }

    // NUEVO: Para Decants en la búsqueda
    public ProductSearchResponse toSearchResponse(DecantPrice decant, Product product) {
        return new ProductSearchResponse(
                decant.id(),
                "DECANT",
                product.id(),
                product.brand(),
                product.line(),
                decant.volumeMl(),
                decant.price().doubleValue(),
                product.allowPromotions()
        );
    }
}
