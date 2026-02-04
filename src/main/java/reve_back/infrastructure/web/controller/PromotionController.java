package reve_back.infrastructure.web.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reve_back.application.ports.in.GetActivePromotionsUseCase;
import reve_back.application.ports.in.SaleSimulationUseCase;
import reve_back.domain.model.CartItem;
import reve_back.domain.model.Promotion;
import reve_back.infrastructure.persistence.enums.global.ItemType;
import reve_back.infrastructure.web.dto.CartItemRequest;
import reve_back.infrastructure.web.dto.SaleSimulationRequest;
import reve_back.infrastructure.web.dto.SaleSimulationResponse;

import java.util.List;

@RestController
@RequestMapping("promotions")
@RequiredArgsConstructor
public class PromotionController {
    private final SaleSimulationUseCase saleuseCase;
    private final GetActivePromotionsUseCase getActivePromotionsUseCase;

    @GetMapping("/active")
    public ResponseEntity<List<Promotion>> getActiveList() {
        // Ejecutamos el caso de uso
        return ResponseEntity.ok(getActivePromotionsUseCase.execute());
    }

    @PostMapping("/calculate")
    public ResponseEntity<SaleSimulationResponse> calculate(
            @RequestBody SaleSimulationRequest request) {

        List<CartItem> domainItems = request.items().stream()
                .map(this::mapToDomain)
                .toList();

        SaleSimulationResponse response = saleuseCase.calculateSimulation(
                domainItems,
                request.promotionId()
        );

        return ResponseEntity.ok(response);
    }

    private CartItem mapToDomain(CartItemRequest dto) {
        return new CartItem(
                dto.tempId(),
                dto.productId(),
                dto.decantPriceId(),
                dto.price(),
                ItemType.valueOf(dto.itemType()),
                dto.manualDiscount(),
                dto.allowPromotions(),
                dto.isPromoForced(),
                false // Inicialmente no está bloqueado
        );
    }

}
