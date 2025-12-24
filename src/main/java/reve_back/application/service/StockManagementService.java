package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.out.*;
import reve_back.domain.model.*;
import reve_back.infrastructure.web.dto.SaleItemRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockManagementService {

    private final BottleRepositoryPort bottleRepositoryPort;
    private final DecantPriceRepositoryPort decantPriceRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;

    @Transactional(rollbackFor = Exception.class)
    public SaleItem processStock(SaleItemRequest req, Long warehouseId, Long userId) {
        if ("BOTELLA".equalsIgnoreCase(req.tipoVendible())) {
            return processBottleSale(req, warehouseId, userId);
        } else {
            return processDecantSale(req, warehouseId, userId);
        }
    }

    private SaleItem processBottleSale(SaleItemRequest req, Long whId, Long userId) {
        Bottle b = bottleRepositoryPort.findById(req.idInventario())
                .orElseThrow(() -> new RuntimeException("Botella no encontrada"));

        int newQty = b.quantity() - req.quantity();
        bottleRepositoryPort.save(new Bottle(b.id(), b.productId(), whId,
                newQty <= 0 ? "AGOTADA" : "SELLADA", b.barcode(), b.volumeMl(), b.remainingVolumeMl(), newQty));

        registerInventoryMovement(b.id(), req.quantity(), "UNIT", userId);

        return new SaleItem(null, b.productId(), null, null, null, req.quantity(), req.price(),
                BigDecimal.ZERO, BigDecimal.ZERO, null, b.volumeMl(), false, false, "NONE");
    }

    private SaleItem processDecantSale(SaleItemRequest req, Long whId, Long userId) {
        DecantPrice dp = decantPriceRepositoryPort.findById(req.idInventario())
                .orElseThrow(() -> new RuntimeException("Precio de decant no encontrado"));

        List<Bottle> bottles = bottleRepositoryPort.findAllByProductId(dp.productId());

        Bottle openBottle = bottles.stream()
                .filter(b -> b.warehouseId().equals(whId) && "DECANTADA".equalsIgnoreCase(b.status()))
                .findFirst()
                .orElseGet(() -> openNewBottle(dp.productId(), whId, userId));

        int volumeNeeded = dp.volumeMl() * req.quantity();

        if (openBottle.remainingVolumeMl() < volumeNeeded) {
            bottleRepositoryPort.save(new Bottle(openBottle.id(), openBottle.productId(), whId,
                    "DECANT_AGOTADA", openBottle.barcode(), openBottle.volumeMl(), 0, 0));
            openBottle = openNewBottle(dp.productId(), whId, userId);
        }

        int nextRemaining = openBottle.remainingVolumeMl() - volumeNeeded;
        bottleRepositoryPort.save(new Bottle(openBottle.id(), openBottle.productId(), whId,
                nextRemaining == 0 ? "DECANT_AGOTADA" : "DECANTADA", openBottle.barcode(),
                openBottle.volumeMl(), nextRemaining, 1));

        registerInventoryMovement(openBottle.id(), volumeNeeded, "ML", userId);

        return new SaleItem(null, null, dp.id(), null, null, req.quantity(), req.price(),
                BigDecimal.ZERO, BigDecimal.ZERO, null, dp.volumeMl(), false, false, "NONE");
    }

    private Bottle openNewBottle(Long productId, Long whId, Long userId) {
        Bottle sealed = bottleRepositoryPort.findAllByProductId(productId).stream()
                .filter(b -> b.warehouseId().equals(whId) && "SELLADA".equalsIgnoreCase(b.status()) && b.quantity() > 0)
                .findFirst().orElseThrow(() -> new RuntimeException("Sin stock sellado para abrir decant"));

        bottleRepositoryPort.save(new Bottle(sealed.id(), sealed.productId(), whId,
                sealed.quantity() - 1 == 0 ? "AGOTADA" : "SELLADA", sealed.barcode(), sealed.volumeMl(), sealed.remainingVolumeMl(), sealed.quantity() - 1));

        registerInventoryMovement(sealed.id(), 1, "UNIT", userId);

        Product p = productRepositoryPort.findById(productId).get();
        return bottleRepositoryPort.save(new Bottle(null, productId, whId, "DECANTADA", null, p.volumeProductsMl(), p.volumeProductsMl(), 1));
    }

    private void registerInventoryMovement(Long bottleId, Integer qty, String unit, Long userId) {
        InventoryMovement movement = new InventoryMovement(null, bottleId, qty, "EGRESO", unit, "VENTA", userId, LocalDateTime.now());
        inventoryMovementRepositoryPort.save(movement);
    }
}