package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reve_back.application.ports.in.CreateProductUseCase;
import reve_back.application.ports.in.ListProductsUseCase;
import reve_back.application.ports.out.BottleRepositoryPort;
import reve_back.application.ports.out.ProductRepositoryPort;
import reve_back.domain.model.Bottle;
import reve_back.domain.model.NewProduct;
import reve_back.domain.model.Product;
import reve_back.infrastructure.web.dto.BottleCreationResponse;
import reve_back.infrastructure.web.dto.ProductCreationRequest;
import reve_back.infrastructure.web.dto.ProductCreationResponse;
import reve_back.infrastructure.web.dto.ProductSummaryDTO;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ProductService implements ListProductsUseCase, CreateProductUseCase {


    private final ProductRepositoryPort productRepositoryPort;
    private final BottleRepositoryPort bottleRepositoryPort;

    @Override
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
    }

    @Override
    public List<ProductSummaryDTO> findAll(int page, int size) {
        return productRepositoryPort.findAll(page, size);
    }
}
