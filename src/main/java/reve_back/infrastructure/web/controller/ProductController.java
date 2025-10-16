package reve_back.infrastructure.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reve_back.application.ports.in.CreateProductUseCase;
import reve_back.application.ports.in.GetProductDetailsUseCase;
import reve_back.application.ports.in.ListProductsUseCase;
import reve_back.application.ports.in.UpdateProductUseCase;
import reve_back.domain.exception.DuplicateBarcodeException;
import reve_back.infrastructure.web.dto.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("products")
public class ProductController {

    private final ListProductsUseCase listProductsUseCase;
    private final GetProductDetailsUseCase getProductDetailsUseCase;
    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;

    @GetMapping
    public List<ProductSummaryDTO> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return listProductsUseCase.findAll(page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductDetails(@PathVariable Long id) {
        try{
            ProductDetailsResponse response = getProductDetailsUseCase.getProductDetails(id);
            return ResponseEntity.ok(response);
        }catch (RuntimeException ex){
            String errorMessage = "Producto no encontrado con ID: " + id;
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
        }
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

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody ProductUpdateRequest request) {
        try{
            ProductDetailsResponse response = updateProductUseCase.updateProduct(id, request);
            return ResponseEntity.ok(response);
        } catch (DuplicateBarcodeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Producto no encontrado con ID: " + id);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error inesperado: " + ex.getMessage());
        }
    }

}
