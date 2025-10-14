package reve_back.infrastructure.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reve_back.application.ports.in.ListProductsUseCase;
import reve_back.infrastructure.web.dto.ProductSummaryDTO;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("products")
public class ProductController {

    private final ListProductsUseCase listProductsUseCase;

    @GetMapping
    public List<ProductSummaryDTO> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return listProductsUseCase.findAll(page, size);
    }
}
