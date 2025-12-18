package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reve_back.application.ports.in.CreateSaleUseCase;
import reve_back.application.ports.out.*;
import reve_back.domain.model.DecantPrice;
import reve_back.infrastructure.persistence.entity.*;
import reve_back.infrastructure.persistence.jpa.BranchJpaRepository;
import reve_back.infrastructure.persistence.jpa.SpringDataBottleRepository;
import reve_back.infrastructure.web.dto.PaymentRequest;
import reve_back.infrastructure.web.dto.SaleCreationRequest;
import reve_back.infrastructure.web.dto.SaleItemRequest;
import reve_back.infrastructure.web.dto.SaleResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class SaleService implements CreateSaleUseCase {

    private final SalesRepositoryPort salesRepositoryPort;
    private final PaymentMethodRepositoryPort paymentMethodRepositoryPort;
    private final CashMovementRepositoryPort cashMovementRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;

    private final SpringDataBottleRepository bottleJpaRepository;
    private final BranchJpaRepository branchJpaRepository;
    private final DecantPriceRepositoryPort decantPriceRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;



    @Override
    public SaleResponse createSale(SaleCreationRequest request) {
        BranchEntity branch = branchJpaRepository.findById(request.branchId())
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));

        Long warehouseId = branch.getWarehouseId();

        SaleEntity sale = new SaleEntity();
        sale.setBranchId(branch.getId());
        sale.setClientId(request.clientId());
        sale.setTotalAmount(request.totalAmount());
        sale.setPromotionId(request.promotionId());

        List<SaleItemEntity> saleItems = new ArrayList<>();

        for (SaleItemRequest itemReq : request.items()) {
            SaleItemEntity itemEntity = new SaleItemEntity();
            itemEntity.setSale(sale);
            itemEntity.setQuantity(itemReq.quantity());
            itemEntity.setPrice(itemReq.price());
            itemEntity.setDiscount(itemReq.discount() != null ? itemReq.discount() : BigDecimal.ZERO);
            itemEntity.setExtraDiscount(itemReq.extraDiscount() != null ? itemReq.extraDiscount() : BigDecimal.ZERO);

            if ("Botella".equalsIgnoreCase(itemReq.tipoVendible())) {
                processBottleSale(itemReq, itemEntity, warehouseId);
            } else if ("Decant".equalsIgnoreCase(itemReq.tipoVendible())) {
                processDecantSale(itemReq, itemEntity, warehouseId);
            }
            saleItems.add(itemEntity);
        }
        sale.setItems(saleItems);

        List<SalePaymentEntity> paymentEntities = new ArrayList<>();
        for (PaymentRequest payReq : request.pagos()) {
            PaymentMethodEntity pm = paymentMethodRepositoryPort.findById(payReq.paymentMethodId())
                    .orElseThrow(() -> new RuntimeException("Método de pago no encontrado"));

            SalePaymentEntity payEntity = new SalePaymentEntity();
            payEntity.setSale(sale);
            payEntity.setPaymentMethod(pm);
            payEntity.setAmount(payReq.amount());
            payEntity.setCommissionApplied(payReq.commissionApplied() != null ? payReq.commissionApplied() : BigDecimal.ZERO);
            paymentEntities.add(payEntity);

            if ("Efectivo".equalsIgnoreCase(pm.getName())) {
                processCashMovement(branch, payReq.amount(), sale);
            }
        }
        sale.setPayments(paymentEntities);

        SaleEntity savedSale = salesRepositoryPort.saveSale(sale);

        if (request.clientId() != null) {
            updateClientVipStatus(request.clientId(), request.totalAmount());
        }

        return new SaleResponse(savedSale.getId(), "Venta registrada con éxito");
    }

    private void processBottleSale(SaleItemRequest item, SaleItemEntity entity, Long warehouseId) {
        BottleEntity bottle = bottleJpaRepository.findById(item.idInventario())
                .orElseThrow(() -> new RuntimeException("Botella no encontrada"));

        if (!bottle.getWarehouseId().equals(warehouseId)) {
            throw new RuntimeException("La botella no pertenece al almacén de esta sucursal");
        }

        // Obtener la capacidad unitaria del producto relacionado
        ProductEntity product = productRepositoryPort.findById(bottle.getProductId());
        int productCapacity = product.getVolumeProductsMl();
        int mlToRemove = productCapacity * item.quantity();

        // REGLA: En sellada cambian AMBOS (Volume y Remaining)
        bottle.setVolumeMl(Math.max(0, bottle.getVolumeMl() - mlToRemove));
        bottle.setRemainingVolumeMl(Math.max(0, bottle.getRemainingVolumeMl() - mlToRemove));

        // Actualizar cantidad física si aplica
        if (bottle.getQuantity() > 0) {
            bottle.setQuantity(bottle.getQuantity() - item.quantity());
        }

        if (bottle.getRemainingVolumeMl() <= 0) {
            bottle.setStatus("agotada");
        }

        bottleJpaRepository.save(bottle);
        entity.setProductId(bottle.getProductId());
    }

    private void processDecantSale(SaleItemRequest item, SaleItemEntity entity, Long warehouseId) {
        DecantPrice decantInfo = decantPriceRepositoryPort.findById(item.idInventario())
                .orElseThrow(() -> new RuntimeException("Precio de decant no encontrado"));

        entity.setDecantPriceId(decantInfo.id());
        int volumeRequired = decantInfo.volumeMl() * item.quantity();
        Long productId = decantInfo.productId();

        List<BottleEntity> bottles = bottleJpaRepository.findByProductId(productId);

        BottleEntity sourceBottle = bottles.stream()
                .filter(b -> b.getWarehouseId().equals(warehouseId))
                .filter(b -> "decantada".equalsIgnoreCase(b.getStatus()))
                .filter(b -> b.getRemainingVolumeMl() > 0)
                .findFirst()
                .orElseGet(() -> openNewBottleFromStock(productId, warehouseId));

        // REGLA: En decantada SOLO cambia el remaining
        if (sourceBottle.getRemainingVolumeMl() >= volumeRequired) {
            sourceBottle.setRemainingVolumeMl(sourceBottle.getRemainingVolumeMl() - volumeRequired);

            if (sourceBottle.getRemainingVolumeMl() == 0) {
                // Solo si llega a 0, ambos quedan en 0 y cambia el estado
                sourceBottle.setVolumeMl(0);
                sourceBottle.setStatus("decant-agotada");
            }
            bottleJpaRepository.save(sourceBottle);
        } else {
            // Lógica de agotamiento y apertura de nueva botella
            sourceBottle.setRemainingVolumeMl(0);
            sourceBottle.setVolumeMl(0);
            sourceBottle.setStatus("decant-agotada");
            bottleJpaRepository.save(sourceBottle);

            BottleEntity nextBottle = openNewBottleFromStock(productId, warehouseId);
            nextBottle.setRemainingVolumeMl(nextBottle.getRemainingVolumeMl() - volumeRequired);
            bottleJpaRepository.save(nextBottle);
        }
    }

    private BottleEntity openNewBottleFromStock(Long productId, Long warehouseId) {
        List<BottleEntity> bottles = bottleJpaRepository.findByProductId(productId);

        BottleEntity newBottle = bottles.stream()
                .filter(b -> b.getWarehouseId().equals(warehouseId))
                .filter(b -> "sellada".equalsIgnoreCase(b.getStatus()))
                .filter(b -> b.getQuantity() > 0)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No hay stock sellado para abrir decants"));

        newBottle.setStatus("decantada");
        newBottle.setRemainingVolumeMl(newBottle.getVolumeMl());
        // Se descuenta 1 unidad de la cantidad física de selladas
        newBottle.setQuantity(newBottle.getQuantity() - 1);

        return bottleJpaRepository.save(newBottle);
    }

    private void processCashMovement(BranchEntity currentBranch, BigDecimal amount, SaleEntity sale) {
        CashMovementEntity movement = new CashMovementEntity();
        movement.setAmount(amount);
        movement.setType("INGRESO");
        movement.setDescription("Venta Sede: " + currentBranch.getName());
        movement.setSaleId(sale.getId());

        if (Boolean.TRUE.equals(currentBranch.getIsCashManagedCentralized())) {
            movement.setBranchId(1L);
        } else {
            movement.setBranchId(currentBranch.getId());
        }

        cashMovementRepositoryPort.save(movement);
    }

    private void updateClientVipStatus(Long clientId, BigDecimal amount) {
        // ... (Tu lógica existente de puntos VIP) ...
        // Puedes reutilizar lo que hiciste en ClientService o inyectarlo
    }
}
