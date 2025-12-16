package reve_back.infrastructure.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reve_back.application.ports.out.BranchRepositoryPort;
import reve_back.domain.model.*;
import reve_back.infrastructure.persistence.entity.ProductEntity;
import reve_back.infrastructure.util.BarcodeGenerator;
import reve_back.infrastructure.web.dto.*;

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public class ProductDtoMapper {

    private final BranchRepositoryPort branchRepositoryPort;

    public BottleCreationResponse toBottleResponse(Bottle b) {
        String branchName = branchRepositoryPort.findAll().stream()
                .filter(branch -> Objects.equals(branch.id(), b.warehouseId()))
                .map(Branch::name)
                .findFirst()
                .orElse("Sede no encontrada");

        return new BottleCreationResponse(
                b.id(),
                b.barcode(),
                branchName,
                b.volumeMl(),
                b.remainingVolumeMl(),
                b.quantity(),
                b.status(),
                BarcodeGenerator.generateBarcodeImageBase64(b.barcode())
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

    public NewProduct toNewProductDomain(ProductCreationRequest request) {
        return new NewProduct(
                request.brand(),
                request.line(),
                request.concentration(),
                request.price(),
                request.unitVolumeMl()
        );
    }

    public Bottle toBottleDomain(BottleCreationRequest req, Long productId) {
        return new Bottle(
                null,
                productId,
                req.status(),
                BarcodeGenerator.generateAlphanumeric(12),
                req.volumeMl(),
                req.remainingVolumeMl(),
                req.quantity(),
                req.branchId()
        );
    }

    public ProductDetailsResponse toProductDetailsResponse(ProductEntity p, List<BottleCreationResponse> bottles, List<DecantResponse> decants) {
        return new ProductDetailsResponse(
                p.getId(),
                p.getBrand(),
                p.getLine(),
                p.getConcentration(),
                p.getPrice(),
                p.getVolumeProductsMl(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
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
