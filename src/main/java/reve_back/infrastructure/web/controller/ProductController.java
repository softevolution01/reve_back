package reve_back.infrastructure.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reve_back.application.ports.in.*;
import reve_back.domain.exception.DuplicateBarcodeException;
import reve_back.domain.exception.DuplicateProductNameException;
import reve_back.infrastructure.web.dto.*;


@RequiredArgsConstructor
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ListProductsUseCase listProductsUseCase;
    private final GetProductDetailsUseCase getProductDetailsUseCase;
    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final ScanBarcodeUseCase scanBarcodeUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('catalog:read:all')")
    public ResponseEntity<ProductPageResponse> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        ProductPageResponse response = listProductsUseCase.findAll(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('catalog:read:detail')")
    public ResponseEntity<?> getProductDetails(@PathVariable Long id) {
        try{
            ProductDetailsResponse response = getProductDetailsUseCase.getProductDetails(id);
            return ResponseEntity.ok(response);
        } catch (DuplicateProductNameException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (RuntimeException ex) {
            if (ex.getMessage().contains("inactivo") || ex.getMessage().contains("no encontrado")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalog:create')")
    public ResponseEntity<?> createProduct(@RequestBody ProductCreationRequest request) {
        try{
            ProductCreationResponse response = createProductUseCase.createProduct(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException ex) {
            String message = ex.getMessage();
            if (message.contains("ya existe")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
            }
            if (message.contains("no hay sedes")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error inesperado");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('catalog:edit')")
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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('catalog:delete')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try{
            deleteProductUseCase.deleteProduct(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            if (ex.getMessage().contains("botellas asociadas")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se puede eliminar: el producto tiene botellas asociadas no agotadas (volume_ml y remaining_volume_ml deben ser 0).");
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Producto no encontrado con ID: " + id);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error inesperado: " + ex.getMessage());
        }
    }

    @GetMapping("/barcode/{barcode}")
    @PreAuthorize("hasAuthority('sales:create:client')")
    public ResponseEntity<ScanBarcodeResponse> scanProduct(@PathVariable String barcode) {
        return ResponseEntity.ok(scanBarcodeUseCase.scanBarcode(barcode));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntime(RuntimeException ex) {
        if (ex.getMessage().contains("Stock agotado")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
        if (ex.getMessage().contains("no encontrado")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno: " + ex.getMessage());
    }

}
