package reve_back.infrastructure.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reve_back.application.ports.in.CreateProductUseCase;
import reve_back.application.ports.in.ListProductsUseCase;
import reve_back.domain.exception.DuplicateBarcodeException;
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
    public ResponseEntity<?> createProduct(@RequestBody ProductCreationRequest request) {
        try{
            ProductCreationResponse response = createProductUseCase.createProduct(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (DuplicateBarcodeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error inesperado: " + ex.getMessage());
        }

    }

}
