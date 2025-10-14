package reve_back.infrastructure.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reve_back.application.ports.in.CreateProductUseCase;
import reve_back.application.ports.in.ListProductsUseCase;
import reve_back.infrastructure.web.dto.ProductCreationRequest;
import reve_back.infrastructure.web.dto.ProductCreationResponse;
import reve_back.infrastructure.web.dto.ProductSummaryDTO;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("products")
public class ProductController {

    private final ListProductsUseCase listProductsUseCase;
    private final CreateProductUseCase createProductUseCase;

    @GetMapping
    public List<ProductSummaryDTO> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return listProductsUseCase.findAll(page, size);
    }

    @PostMapping
    public ProductCreationResponse createProduct(@RequestBody ProductCreationRequest request) {
        return createProductUseCase.createProduct(request);
    }

}
