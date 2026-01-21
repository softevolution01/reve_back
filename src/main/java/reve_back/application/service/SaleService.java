package reve_back.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.in.CreateSaleUseCase;
import reve_back.application.ports.out.*;
import reve_back.domain.model.*;
import reve_back.infrastructure.persistence.entity.ClientLoyaltyProgressEntity;
import reve_back.infrastructure.web.dto.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private final ClientRepositoryPort clientRepositoryPort;
    private final PaymentMethodsRepositoryPort paymentMethodRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final LoyaltyTiersRepositoryPort loyaltyTiersRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long execute(SaleSimulationRequest request) {
        return 0L;
    }

    private static final Logger log = LoggerFactory.getLogger(SaleService.class);

    @Transactional(rollbackFor = Exception.class)
    public SaleResponse createSale(SaleCreationRequest request) {
        Long finalClientId = request.clientId() != null ? request.clientId() : 1L;

        User seller = userRepositoryPort.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("Usuario (Vendedor) no encontrado"));
        String sellerName = seller.fullname();

        Client client = clientRepositoryPort.findById(finalClientId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        String clientName = client.fullname(); // Obtenemos el nombre para la respuesta
        String clientDNI = client.dni();

        Branch branch = branchRepositoryPort.findById(request.branchId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));
        Long warehouseId = branch.warehouseId();

        List<SalePayment> salePayments = new ArrayList<>();
        BigDecimal totalSurcharge = BigDecimal.ZERO;
        Set<String> methodNames = new HashSet<>();

        for (PaymentRequest pReq : request.payments()) {
            PaymentMethod pm = paymentMethodRepositoryPort.findById(pReq.paymentMethodId())
                    .orElseThrow(() -> new RuntimeException("Método de pago no encontrado"));

            methodNames.add(pm.name().toUpperCase());

            BigDecimal surcharge = BigDecimal.ZERO;
            if (pm.surchargePercentage() != null && pm.surchargePercentage().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal divisor = new BigDecimal("100").add(pm.surchargePercentage());
                surcharge = pReq.amount()
                        .multiply(pm.surchargePercentage())
                        .divide(divisor, 2, RoundingMode.HALF_UP);

                totalSurcharge = totalSurcharge.add(surcharge);
            }

            salePayments.add(new SalePayment(null, pReq.paymentMethodId(), pm.name(), pReq.amount(), surcharge));

            // Movimientos de caja (Efectivo)
            if (pm.name().equalsIgnoreCase("Efectivo")) {
                Long destBranchId = Boolean.TRUE.equals(branch.isCashManagedCentralized()) ? 1L : branch.id();
                CashMovement movement = new CashMovement(null, destBranchId, pReq.amount(),
                        "INGRESO", "Venta en Sede: " + branch.name(), request.userId(), null, LocalDateTime.now());
                cashMovementRepositoryPort.save(movement);
            }
        }
        String paymentMethodName = (methodNames.size() > 1) ? "MIXTO" : methodNames.iterator().next();

        // 3. Procesamiento de Items (Lógica de Stock y Descuentos)
        List<SaleItem> tempItems = new ArrayList<>();
        BigDecimal totalManualDiscount = BigDecimal.ZERO;

        for (SaleItemRequest itemReq : request.items()) {
            SaleItem domainItem = processStockLogic(itemReq, warehouseId, request.userId());

            BigDecimal itemManualDiscount = itemReq.manualDiscount() != null ? itemReq.manualDiscount() : BigDecimal.ZERO;
            totalManualDiscount = totalManualDiscount.add(itemManualDiscount);

            // Subtotal preliminar
            tempItems.add(new SaleItem(
                    null,
                    domainItem.productId(),
                    domainItem.decantPriceId(),
                    domainItem.productName(),
                    domainItem.productBrand(),
                    domainItem.quantity(), domainItem.unitPrice(),
                    BigDecimal.ZERO, // System discount placeholder
                    itemManualDiscount,
                    BigDecimal.ZERO, // Final subtotal placeholder
                    domainItem.volumeMlPerUnit(),
                    itemReq.blockedPromo() != null ? itemReq.blockedPromo() : false,
                    false, "NONE"
            ));
        }

        // 4. Aplicación de Descuentos de Sistema (Promociones Globales)
        BigDecimal remainingSystemDiscount = request.systemDiscount() != null ? request.systemDiscount() : BigDecimal.ZERO;

        if (remainingSystemDiscount.compareTo(BigDecimal.ZERO) > 0) {
            List<Integer> promoIndices = new ArrayList<>();
            for (int i = 0; i < tempItems.size(); i++) {
                if (tempItems.get(i).isPromoLocked()) {
                    promoIndices.add(i);
                }
            }
            promoIndices.sort(Comparator.comparing(i -> tempItems.get(i).unitPrice()));

            for (Integer index : promoIndices) {
                if (remainingSystemDiscount.compareTo(BigDecimal.ZERO) <= 0) break;

                SaleItem currentItem = tempItems.get(index);
                BigDecimal lineGross = currentItem.unitPrice().multiply(new BigDecimal(currentItem.quantity()));
                BigDecimal discountToApply = lineGross.min(remainingSystemDiscount);

                tempItems.set(index, new SaleItem(
                        currentItem.id(), currentItem.productId(), currentItem.decantPriceId(), currentItem.productName(), currentItem.productBrand(),
                        currentItem.quantity(), currentItem.unitPrice(),
                        discountToApply,
                        currentItem.manualDiscount(),
                        BigDecimal.ZERO,
                        currentItem.volumeMlPerUnit(), currentItem.isPromoLocked(), currentItem.isPromoForced(), currentItem.promoStrategyApplied()
                ));

                remainingSystemDiscount = remainingSystemDiscount.subtract(discountToApply);
            }
        }

        // 5. Cálculo de Totales Finales
        List<SaleItem> finalSaleItems = new ArrayList<>();
        BigDecimal totalBruto = BigDecimal.ZERO;
        BigDecimal totalSystemDiscountApplied = BigDecimal.ZERO;
        BigDecimal totalLoyaltyAmount = BigDecimal.ZERO;

        for (SaleItem item : tempItems) {
            BigDecimal lineGrossTotal = item.unitPrice().multiply(new BigDecimal(item.quantity()));

            // Total Neto por línea (Precio x Cantidad - Descuentos)
            BigDecimal lineNetTotal = lineGrossTotal
                    .subtract(item.manualDiscount())
                    .subtract(item.systemDiscount());

            finalSaleItems.add(new SaleItem(
                    item.id(), item.productId(), item.decantPriceId(), item.productName(), item.productBrand(),
                    item.quantity(), item.unitPrice(), item.systemDiscount(), item.manualDiscount(),
                    lineNetTotal, // Guardamos el neto real
                    item.volumeMlPerUnit(), item.isPromoLocked(), item.isPromoForced(), item.promoStrategyApplied()
            ));

            totalBruto = totalBruto.add(lineGrossTotal);
            totalSystemDiscountApplied = totalSystemDiscountApplied.add(item.systemDiscount());

            if (item.decantPriceId() != null) {
                totalLoyaltyAmount = totalLoyaltyAmount.add(lineNetTotal);
            }
        }

        BigDecimal totalDiscountGlobal = totalManualDiscount.add(totalSystemDiscountApplied);

        BigDecimal totalNetoMerchandise = totalBruto.subtract(totalDiscountGlobal);

        BigDecimal totalFinalCharged = totalNetoMerchandise.add(totalSurcharge);

        // 6. Guardado en Base de Datos
        Sale saleToSave = new Sale(null, LocalDateTime.now(), branch.id(), request.userId(), finalClientId,
                null, totalBruto, totalDiscountGlobal, new BigDecimal("0.18"), totalSurcharge, totalFinalCharged,
                paymentMethodName, finalSaleItems, salePayments);

        Sale savedSale = salesRepositoryPort.save(saleToSave);
        log.info("✅ Venta Guardada con ID: {}", savedSale.id());

        try {
            boolean triggerVipReset = updateClientVipStatus(finalClientId);
            updateLoyalty(finalClientId, totalLoyaltyAmount, triggerVipReset);
        } catch (Exception e) {
            log.error("Error Post-Venta: {}", e.getMessage());
        }

        List<SaleItemResponse> itemsResponse = finalSaleItems.stream()
                .map(item -> {
                    // A. Tipo Visual
                    String typeDisplay = "Botella";
                    if (item.volumeMlPerUnit() != null) {
                        typeDisplay = item.volumeMlPerUnit().intValue() + "ml";
                    }

                    String safeProductName = item.productName() != null ? item.productName() : "SIN NOMBRE";

                    String prefix = (item.volumeMlPerUnit() == null) ? "BT" : "DC";

                    String finalDisplayName = prefix + " " + safeProductName;

                    // C. Descuento Linea
                    BigDecimal totalDiscLine = (item.manualDiscount() == null ? BigDecimal.ZERO : item.manualDiscount())
                            .add(item.systemDiscount() == null ? BigDecimal.ZERO : item.systemDiscount());

                    return new SaleItemResponse(
                            finalDisplayName, // <--- Aquí va el nombre con prefijo
                            typeDisplay,
                            item.quantity(),
                            item.unitPrice(),
                            item.finalSubtotal(),
                            totalDiscLine,
                            item.isPromoForced()
                    );
                })
                .collect(Collectors.toList());

        BigDecimal baseImponible = totalNetoMerchandise.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);

        return new SaleResponse(
                savedSale.id(),
                savedSale.saleDate(),
                branch.name(),
                sellerName,
                clientName,
                clientDNI,
                totalBruto,
                totalDiscountGlobal,
                totalSurcharge,
                totalFinalCharged,
                baseImponible,
                paymentMethodName, // <--- AGREGADO: Método de Pago
                itemsResponse
        );
    }

    private SaleItem processStockLogic(SaleItemRequest req, Long whId, Long userId) {
        if ("BOTELLA".equalsIgnoreCase(req.tipoVendible())) {
            log.info("Lógica de Stock: BOTELLA SELLADA. ID: {}", req.idInventario());

            Bottle b = bottleRepositoryPort.findById(req.idInventario())
                    .orElseThrow(() -> new RuntimeException("Botella no encontrada"));

            int newQty = b.quantity() - req.quantity();
            log.info("    Descontando Botella ID {}. Cantidad Anterior: {}, Descuento: {}, Nueva: {}",
                    b.id(), b.quantity(), req.quantity(), newQty);

            bottleRepositoryPort.save(new Bottle(b.id(), b.productId(), whId,
                    newQty <= 0 ? "AGOTADA" : "SELLADA", b.barcode(), b.volumeMl(), b.remainingVolumeMl(), newQty));

            registerInventoryMovement(b.id(), req.quantity(), "UNIT", userId);

            return new SaleItem(null, b.productId(), null, null, null, req.quantity(), req.price(),
                    BigDecimal.ZERO, BigDecimal.ZERO, null, b.volumeMl(), false, false, "NONE");
        } else {
            log.info("Lógica de Stock: DECANT (Por ML). ID Precio Decant: {}", req.idInventario());
            return handleDecantStock(req, whId, userId);
        }
    }

    private SaleItem handleDecantStock(SaleItemRequest req, Long whId, Long userId) {
        DecantPrice dp = decantPriceRepositoryPort.findById(req.idInventario())
                .orElseThrow(() -> new RuntimeException("Precio de decant no encontrado"));

        // 1. Buscamos la botella decantada
        Bottle decantBottle = bottleRepositoryPort.findAllByProductId(dp.productId()).stream()
                .filter(b -> b.warehouseId().equals(whId) &&
                        ("DECANTADA".equalsIgnoreCase(b.status()) || "DECANT_AGOTADA".equalsIgnoreCase(b.status())))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No existe registro de botella decantada para inicializar."));

        log.info("    Botella Decantada encontrada: ID {}, Status: {}, Remanente Actual: {}ml",
                decantBottle.id(), decantBottle.status(), decantBottle.remainingVolumeMl());

        Product product = productRepositoryPort.findById(dp.productId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // Verificación de estado agotado previo
        if ("DECANT_AGOTADA".equalsIgnoreCase(decantBottle.status()) || decantBottle.remainingVolumeMl() == 0) {
            log.info("    ⚠️ Botella estaba AGOTADA. Iniciando reabastecimiento forzoso...");
            replenishDecantBottle(decantBottle, userId);

            // CORRECCIÓN CRÍTICA: Recargar la entidad desde la BD tras el reabastecimiento
            decantBottle = bottleRepositoryPort.findById(decantBottle.id())
                    .orElseThrow(() -> new RuntimeException("Error recargando botella tras replenish"));
            log.info("    ✅ Botella Recargada. Nuevo Remanente: {}ml", decantBottle.remainingVolumeMl());
        }

        int volumeNeeded = dp.volumeMl() * req.quantity();
        int currentRem = decantBottle.remainingVolumeMl();

        log.info("    Calculando: Se necesitan {}ml. Hay disponible {}ml", volumeNeeded, currentRem);

        if (currentRem < volumeNeeded) {
            log.info("    >> ESCENARIO A: Stock insuficiente. Realizando consumo split.");
            int volumeDeficit = volumeNeeded - currentRem;

            log.info("       1. Consumiendo remanente de {}ml de botella ID {}", currentRem, decantBottle.id());
            registerInventoryMovement(decantBottle.id(), currentRem, "ML", userId);

            log.info("       2. Reabasteciendo desde botella sellada...");
            replenishDecantBottle(decantBottle, userId);
            Bottle refreshedBottle = bottleRepositoryPort.findById(decantBottle.id()).orElseThrow();

            int finalRemaining = refreshedBottle.remainingVolumeMl() - volumeDeficit;
            log.info("       3. Consumiendo déficit de {}ml. Remanente final será: {}ml", volumeDeficit, finalRemaining);

            bottleRepositoryPort.save(new Bottle(
                    refreshedBottle.id(), refreshedBottle.productId(), whId,
                    finalRemaining <= 0 ? "DECANT_AGOTADA" : "DECANTADA",
                    refreshedBottle.barcode(), refreshedBottle.volumeMl(),
                    finalRemaining,
                    1
            ));

            registerInventoryMovement(decantBottle.id(), req.quantity(), "ML", userId);

        } else {
            log.info("    >> ESCENARIO B: Stock suficiente.");
            int nextRemaining = currentRem - volumeNeeded;

            bottleRepositoryPort.save(new Bottle(
                    decantBottle.id(), decantBottle.productId(), whId,
                    nextRemaining == 0 ? "DECANT_AGOTADA" : "DECANTADA",
                    decantBottle.barcode(), decantBottle.volumeMl(),
                    nextRemaining,
                    1
            ));

            registerInventoryMovement(decantBottle.id(), req.quantity(), "ML", userId);
        }

        return new SaleItem(null, dp.productId(), dp.id(), product.brand()+product.line(), null, req.quantity(), req.price(),
                BigDecimal.ZERO, BigDecimal.ZERO, null, dp.volumeMl(), req.blockedPromo(), req.forcePromo(), "NONE");
    }

    private void replenishDecantBottle(Bottle targetDecantBottle, Long userId) {
        Long whId = targetDecantBottle.warehouseId();
        Long productId = targetDecantBottle.productId();

        log.info("=== REPLENISH LOGIC START ===");
        log.info("Target Decant ID: {}, ProductID: {}, WarehouseID: {}", targetDecantBottle.id(), productId, whId);

        // 1. Búsqueda de candidatos (Igual que antes)
        List<Bottle> candidates = bottleRepositoryPort.findAllByProductId(productId);

        Bottle sealedBottle = candidates.stream()
                .filter(b -> b.warehouseId().equals(whId) &&
                        "SELLADA".equalsIgnoreCase(b.status() != null ? b.status().trim() : "") &&
                        b.quantity() > 0)
                .findFirst()
                .orElseThrow(() -> {
                    log.error("❌ ERROR: No hay stock SELLADO para reponer.");
                    return new RuntimeException("No hay stock SELLADO para reponer el Decant");
                });

        log.info("✅ Candidata seleccionada: Botella ID {}", sealedBottle.id());

        // ---------------------------------------------------------------
        // 2. Actualizar la SELLADA (Restar 1 y ajustar volúmenes si es 0)
        // ---------------------------------------------------------------
        int newSealedQty = sealedBottle.quantity() - 1;
        boolean isDepleted = newSealedQty == 0; // ¿Se acabó?

        bottleRepositoryPort.save(new Bottle(
                sealedBottle.id(),
                sealedBottle.productId(),
                whId,
                isDepleted ? "AGOTADA" : "SELLADA", // Nuevo Estado
                sealedBottle.barcode(),
                isDepleted ? 0 : sealedBottle.volumeMl(),
                isDepleted ? 0 : sealedBottle.remainingVolumeMl(),
                newSealedQty
        ));

        log.info("Stock Sellado actualizado. Nueva cantidad: {}. Volúmenes en 0? {}", newSealedQty, isDepleted);

        registerInventoryMovement(sealedBottle.id(), 1, "UNIT", userId);

        Product product = productRepositoryPort.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado al reabastecer"));

        // B. Obtenemos el volumen oficial (Ej: 100ml, 750ml)
        // Asumo que tu entidad Product tiene un método volumeMl() o similar.
        int productOfficialVolume = product.volumeProductsMl();

        log.info("Recargando Decant ID {}. Volumen Oficial del Producto: {}ml",
                targetDecantBottle.id(), productOfficialVolume);

        Bottle refreshedBottle = new Bottle(
                targetDecantBottle.id(),
                productId,
                whId,
                "DECANTADA",
                targetDecantBottle.barcode(),
                productOfficialVolume, // CAPACIDAD: Viene del Producto
                productOfficialVolume, // REMANENTE: Empieza Lleno igual al producto
                1
        );

        bottleRepositoryPort.save(refreshedBottle);
        log.info("=== REPLENISH LOGIC END ===");
    }

    private void registerInventoryMovement(Long bottleId, Integer qty, String unit, Long userId) {
        log.debug("Registrando movimiento: Botella {}, Qty {}, Unit {}", bottleId, qty, unit);
        InventoryMovement movement = new InventoryMovement(null, bottleId, qty, "EGRESO", unit, "VENTA", userId, LocalDateTime.now());
        inventoryMovementRepositoryPort.save(movement);
    }

    private void updateLoyalty(Long clientId, BigDecimal amount, boolean resetProgress) {
        if (clientId == 1L) return;

        // 1. Obtener Objeto de Dominio (El puerto devuelve Domain, no Entity)
        ClientLoyaltyProgress progress = loyaltyProgressRepositoryPort.findByClientId(clientId)
                .orElse(null);

        BigDecimal totalHistoryMoney = BigDecimal.ZERO;

        // 2. RECUPERAR HISTÓRICO (Usando getters del dominio)
        if (progress != null && !resetProgress) {
            totalHistoryMoney = progress.accumulatedMoney();
        }

        // 3. SUMAR LA COMPRA ACTUAL
        totalHistoryMoney = totalHistoryMoney.add(amount);

        // =================================================================================
        // 4. BUCLE DE CÁLCULO (Simulación desde Nivel 1)
        // =================================================================================
        BigDecimal remainingMoney = totalHistoryMoney;
        int calculatedTier = 1;

        while (true) {
            // A. Configuración del nivel (El puerto devuelve Domain de LoyaltyTier)
            LoyaltyTiers tierConfig = loyaltyTiersRepositoryPort.findByTierLevel(calculatedTier)
                    .orElse(null);

            // Si se acabaron los niveles, paramos.
            if (tierConfig == null) {
                calculatedTier--;
                break;
            }

            // Calculamos Meta: Costo * 6
            BigDecimal levelGoal = tierConfig.costPerPoint().multiply(new BigDecimal("6"));

            // B. ¿El dinero cubre este nivel completo?
            if (remainingMoney.compareTo(levelGoal) >= 0) {

                if (loyaltyTiersRepositoryPort.existsByTierLevel(calculatedTier + 1)) {
                    // SÍ: Pagamos y subimos
                    remainingMoney = remainingMoney.subtract(levelGoal);
                    calculatedTier++;
                    log.info("🚀 Sube al Nivel {}", calculatedTier);
                } else {
                    // Tope alcanzado
                    break;
                }
            } else {
                // NO: Se queda en este nivel
                break;
            }
        }

        // =================================================================================
        // 5. LÓGICA B: GUARDAR DINERO SOBRANTE COMO "PUNTOS"
        // =================================================================================
        // Ejemplo: 400 total - 360 costo = 40 sobrantes. Guardamos 40.
        int pointsToSave = remainingMoney.intValue();

        // 6. GUARDAR (Pasamos el Objeto de Dominio al Puerto)
        // El puerto/adaptador se encargará de usar el Mapper para convertirlo a Entity.
        ClientLoyaltyProgress domainToSave = new ClientLoyaltyProgress(
                clientId,
                calculatedTier,
                pointsToSave,        // Guardamos el remanente (40)
                totalHistoryMoney,   // Guardamos el histórico total (400)
                LocalDateTime.now()
        );

        loyaltyProgressRepositoryPort.save(domainToSave);

        log.info("✅ Loyalty Actualizado (Dominio) -> Cliente: {}, Nivel: {}, Puntos/Sobrante: {}, Total: {}",
                clientId, calculatedTier, pointsToSave, totalHistoryMoney);
    }

    private boolean updateClientVipStatus(Long clientId) {
        if (clientId == 1L) return false;

        Client client = clientRepositoryPort.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        int currentCounter = client.vipPurchaseCounter();
        int newCounter = currentCounter + 1;

        boolean wasVip = client.isVip();
        boolean isNowVip = wasVip;       // Por defecto, mantenemos el estado actual (si era VIP, sigue VIP)
        LocalDateTime vipSince = client.vipSince();
        boolean shouldResetLoyalty = false;

        if (!wasVip && newCounter == 2) {
            isNowVip = true;
            shouldResetLoyalty = true;
            vipSince = LocalDateTime.now();
            log.info("🌟 Cliente {} llega a 2 compras: Se vuelve VIP y se resetea su lealtad.", client.fullname());
        }

        clientRepositoryPort.save(new Client(
                client.id(),
                client.fullname(),
                client.dni(),
                client.email(),
                client.phone(),
                isNowVip,
                vipSince,
                newCounter,
                client.createdAt()
        ));

        return shouldResetLoyalty;
    }
}