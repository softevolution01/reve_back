package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.in.CreateSaleUseCase;
import reve_back.application.ports.out.*;
import reve_back.domain.model.*;
import reve_back.infrastructure.web.dto.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService implements CreateSaleUseCase {

    private final SalesRepositoryPort salesRepositoryPort;
    private final BranchRepositoryPort branchRepositoryPort;
    private final BottleRepositoryPort bottleRepositoryPort;
    private final DecantPriceRepositoryPort decantPriceRepositoryPort;
    private final CashMovementRepositoryPort cashMovementRepositoryPort;
    private final LoyaltyProgressRepositoryPort loyaltyProgressRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final PaymentMethodsRepositoryPort paymentMethodRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long execute(SaleSimulationRequest request) {
        return 0L;
    }

    @Transactional(rollbackFor = Exception.class)
    public SaleResponse createSale(SaleCreationRequest request) {
        Branch branch = branchRepositoryPort.findById(request.branchId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        Long warehouseId = branch.warehouseId();

        List<SaleItem> saleItems = new ArrayList<>();
        BigDecimal totalBruto = BigDecimal.ZERO;

        for (SaleItemRequest itemReq : request.items()) {
            SaleItem domainItem = processStockLogic(itemReq, warehouseId, request.userId());
            saleItems.add(domainItem);

            BigDecimal lineTotal = domainItem.unitPrice().multiply(new BigDecimal(domainItem.quantity()));
            totalBruto = totalBruto.add(lineTotal);
        }

        BigDecimal totalNeto = totalBruto.subtract(request.manualDiscount() != null ? request.manualDiscount() : BigDecimal.ZERO);

        List<SalePayment> salePayments = new ArrayList<>();

        for (PaymentRequest pReq : request.payments()) {
            PaymentMethod pm = paymentMethodRepositoryPort.findById(pReq.paymentMethodId())
                    .orElseThrow(() -> new RuntimeException("Método de pago no encontrado"));

            salePayments.add(new SalePayment(null, pReq.paymentMethodId(), pm.name(), pReq.amount(), pReq.commission()));

            if (pm.name().equalsIgnoreCase("Efectivo")) {
                Long destBranchId = Boolean.TRUE.equals(branch.isCashManagedCentralized()) ? 1L : branch.id();

                CashMovement movement = new CashMovement(null, destBranchId, pReq.amount(),
                        "INGRESO", "Venta en Sede: " + branch.name(), request.userId(), null, LocalDateTime.now());
                cashMovementRepositoryPort.save(movement);
            }
        }

        Sale saleToSave = new Sale(null, LocalDateTime.now(), branch.id(), request.userId(), request.clientId(),
                null, totalBruto, BigDecimal.ZERO, new BigDecimal("0.18"), BigDecimal.ZERO, totalNeto,
                "MIXTO", saleItems, salePayments);

        Sale savedSale = salesRepositoryPort.save(saleToSave);

        if (request.clientId() != null) {
            updateLoyalty(request.clientId(), totalNeto);
        }

        return new SaleResponse(savedSale.id(), savedSale.saleDate(), branch.name(), "Vendedor", "Cliente",
                totalBruto, BigDecimal.ZERO, BigDecimal.ZERO, totalNeto, totalNeto, null);
    }

    private SaleItem processStockLogic(SaleItemRequest req, Long whId, Long userId) {
        if ("BOTELLA".equalsIgnoreCase(req.tipoVendible())) {
            Bottle b = bottleRepositoryPort.findById(req.idInventario())
                    .orElseThrow(() -> new RuntimeException("Botella no encontrada"));

            int newQty = b.quantity() - req.quantity();
            bottleRepositoryPort.save(new Bottle(b.id(), b.productId(), whId,
                    newQty <= 0 ? "AGOTADA" : "SELLADA", b.barcode(), b.volumeMl(), b.remainingVolumeMl(), newQty));

            registerInventoryMovement(b.id(), req.quantity(), "UNIT", userId);

            return new SaleItem(null, b.productId(), null, null, null, req.quantity(), req.price(),
                    BigDecimal.ZERO, BigDecimal.ZERO, null, b.volumeMl(), false, false, "NONE");
        } else {
            return handleDecantStock(req, whId, userId);
        }
    }

    private SaleItem handleDecantStock(SaleItemRequest req, Long whId, Long userId) {
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

    private void updateLoyalty(Long clientId, BigDecimal amount) {
        ClientLoyaltyProgress progress = loyaltyProgressRepositoryPort.findByClientId(clientId)
                .orElse(new ClientLoyaltyProgress(clientId, 1, 0, BigDecimal.ZERO, LocalDateTime.now()));

        BigDecimal total = progress.accumulatedMoney().add(amount);
        loyaltyProgressRepositoryPort.save(new ClientLoyaltyProgress(clientId, progress.currentTier(),
                progress.pointsInTier(), total, LocalDateTime.now()));
    }
}