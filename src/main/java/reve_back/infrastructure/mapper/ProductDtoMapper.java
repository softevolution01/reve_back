package reve_back.infrastructure.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reve_back.application.ports.out.BranchRepositoryPort;
import reve_back.application.ports.out.WarehouseRepositoryPort;
import reve_back.domain.model.*;
import reve_back.infrastructure.persistence.entity.DecantPriceEntity;
import reve_back.infrastructure.persistence.entity.ProductEntity;
import reve_back.infrastructure.util.BarcodeGenerator;
import reve_back.infrastructure.web.dto.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public class ProductDtoMapper {

    private final WarehouseRepositoryPort warehouseRepositoryPort;

    public Product toDomain(ProductCreationRequest request) {
        return new Product(
                null,
                request.brand().toUpperCase().trim(),
                request.price(),
                request.line().toUpperCase().trim(),
                request.concentration().toUpperCase().trim(),
                request.unitVolumeMl(),
                true, // is_active
                null, // createdAt
                null  // updatedAt
        );
    }

    public Product toDomain(Long id, ProductUpdateRequest request, Product existing) {
        return new Product(
                id,
                request.brand().toUpperCase().trim(),
                request.price(),
                request.line().toUpperCase().trim(),
                request.concentration().toUpperCase().trim(),
                request.unitVolumeMl(),
                existing.isActive(), // Mantenemos el estado actual
                existing.createdAt(), // Mantenemos fecha creación
                null // El updatedAt lo pondrá Hibernate
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
                "Botella",
                product.id(),
                product.brand(),
                product.line(),
                bottle.volumeMl(),
                priceOverride
        );
    }

    public ScanBarcodeResponse toScanResponse(DecantPrice decant, Product product) {
        return new ScanBarcodeResponse(
                decant.id(),
                "Decant",
                product.id(),
                product.brand(),
                product.line(),
                decant.volumeMl(),
                decant.price()
        );
    }
}
