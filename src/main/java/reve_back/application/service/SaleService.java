package reve_back.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.in.CreateSaleUseCase;
import reve_back.application.ports.out.*;
import reve_back.domain.model.*;
import reve_back.infrastructure.web.dto.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long execute(SaleSimulationRequest request) {
        return 0L;
    }

    private static final Logger log = LoggerFactory.getLogger(SaleService.class);

    @Transactional(rollbackFor = Exception.class)
    public SaleResponse createSale(SaleCreationRequest request) {
        Long finalClientId = request.clientId() != null ? request.clientId() : 1L;
        log.info(">>> INICIO PROCESO DE VENTA - BranchId: {}, UserId: {}, ClientId: {}",
                request.branchId(), request.userId(), finalClientId);

        Branch branch = branchRepositoryPort.findById(request.branchId())
                .orElseThrow(() -> {
                    log.error("❌ Sede no encontrada: {}", request.branchId());
                    return new RuntimeException("Sede no encontrada");
                });
        Long warehouseId = branch.warehouseId();

        // --- NUEVO: CÁLCULO PREVIO DE RECARGOS (Para cumplir con Stefany) ---
        List<SalePayment> salePayments = new ArrayList<>();
        BigDecimal totalSurcharge = BigDecimal.ZERO;
        Set<String> methodNames = new HashSet<>();

        for (PaymentRequest pReq : request.payments()) {
            PaymentMethod pm = paymentMethodRepositoryPort.findById(pReq.paymentMethodId())
                    .orElseThrow(() -> new RuntimeException("Método de pago no encontrado"));

            methodNames.add(pm.name().toUpperCase());

            // Recargo Tarjeta (5%)
            BigDecimal surcharge = BigDecimal.ZERO;
            if (pm.surchargePercentage() != null && pm.surchargePercentage().compareTo(BigDecimal.ZERO) > 0) {
                surcharge = pReq.amount().multiply(pm.surchargePercentage())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                totalSurcharge = totalSurcharge.add(surcharge);
            }

            salePayments.add(new SalePayment(null, pReq.paymentMethodId(), pm.name(), pReq.amount(), surcharge));

            if (pm.name().equalsIgnoreCase("Efectivo")) {
                Long destBranchId = Boolean.TRUE.equals(branch.isCashManagedCentralized()) ? 1L : branch.id();
                CashMovement movement = new CashMovement(null, destBranchId, pReq.amount(),
                        "INGRESO", "Venta en Sede: " + branch.name(), request.userId(), null, LocalDateTime.now());
                cashMovementRepositoryPort.save(movement);
            }
        }
        // Reconocer método enviado
        String paymentMethodName = (methodNames.size() > 1) ? "MIXTO" : methodNames.iterator().next();

        // --- TU LÓGICA DE ÍTEMS (MANTENIDA EXACTAMENTE IGUAL) ---
        List<SaleItem> saleItems = new ArrayList<>();
        BigDecimal totalBruto = BigDecimal.ZERO;
        BigDecimal totalManualDiscount = BigDecimal.ZERO;

        int itemCount = 0;
        for (SaleItemRequest itemReq : request.items()) {
            itemCount++;
            log.info("--- Procesando Item #{} - ID Inventario: {}, Tipo: {}, Cantidad: {} ---",
                    itemCount, itemReq.idInventario(), itemReq.tipoVendible(), itemReq.quantity());

            SaleItem domainItem = processStockLogic(itemReq, warehouseId, request.userId());

            BigDecimal itemManualDiscount = itemReq.manualDiscount() != null ? itemReq.manualDiscount() : BigDecimal.ZERO;
            BigDecimal lineGrossTotal = domainItem.unitPrice().multiply(new BigDecimal(domainItem.quantity()));
            BigDecimal lineNetTotal = lineGrossTotal.subtract(itemManualDiscount);

            log.info("    Item Procesado: Total Bruto: {}, Descuento Manual: {}, Total Neto: {}",
                    lineGrossTotal, itemManualDiscount, lineNetTotal);

            SaleItem finalItem = new SaleItem(
                    null,
                    domainItem.productId(),
                    domainItem.decantPriceId(),
                    domainItem.productName(),
                    domainItem.productBrand(),
                    domainItem.quantity(),
                    domainItem.unitPrice(),
                    BigDecimal.ZERO,
                    itemManualDiscount,
                    lineNetTotal, // Se ajustará con recargo abajo
                    domainItem.volumeMlPerUnit(),
                    itemReq.blockedPromo() != null ? itemReq.blockedPromo() : false,
                    false, "NONE"
            );

            saleItems.add(finalItem);
            totalBruto = totalBruto.add(lineGrossTotal);
            totalManualDiscount = totalManualDiscount.add(itemManualDiscount);
        }

        // Cálculos Financieros Finales
        BigDecimal totalSystemDiscount = request.systemDiscount() != null ? request.systemDiscount() : BigDecimal.ZERO;
        BigDecimal totalDiscountGlobal = totalManualDiscount.add(totalSystemDiscount);
        BigDecimal totalNeto = totalBruto.subtract(totalDiscountGlobal);

        // NUEVO: total_final_charged = neto + recargo
        BigDecimal totalFinalCharged = totalNeto.add(totalSurcharge);

        // NUEVO: Repartir el recargo proporcionalmente en los items
        if (totalSurcharge.compareTo(BigDecimal.ZERO) > 0 && totalNeto.compareTo(BigDecimal.ZERO) > 0) {
            for (int i = 0; i < saleItems.size(); i++) {
                SaleItem item = saleItems.get(i);
                BigDecimal share = item.finalSubtotal().divide(totalNeto, 4, RoundingMode.HALF_UP).multiply(totalSurcharge);
                saleItems.set(i, new SaleItem(
                        item.id(), item.productId(), item.decantPriceId(), item.productName(), item.productBrand(),
                        item.quantity(), item.unitPrice(), item.systemDiscount(), item.manualDiscount(),
                        item.finalSubtotal().add(share).setScale(2, RoundingMode.HALF_UP), // <--- RECARGO SUMADO
                        item.volumeMlPerUnit(), item.isPromoLocked(), item.isPromoForced(), item.promoStrategyApplied()
                ));
            }
        }

        log.info("Resumen Financiero - Bruto: {}, Desc. Global: {}, Recargo: {}, FINAL: {}",
                totalBruto, totalDiscountGlobal, totalSurcharge, totalFinalCharged);

        // Guardado de la Venta (Con tus variables)
        Sale saleToSave = new Sale(null, LocalDateTime.now(), branch.id(), request.userId(), finalClientId,
                null, totalBruto, totalDiscountGlobal, new BigDecimal("0.18"), totalSurcharge, totalFinalCharged,
                paymentMethodName, saleItems, salePayments);

        Sale savedSale = salesRepositoryPort.save(saleToSave);
        log.info("✅ Venta Guardada con ID: {}", savedSale.id());

        // VIP y Loyalty intactos (Usamos totalFinalCharged para los puntos)
        try { updateClientVipStatus(finalClientId); } catch (Exception e) { log.error("Error VIP: {}", e.getMessage()); }
        try { updateLoyalty(finalClientId, totalFinalCharged); } catch (Exception e) { log.error("Error Loyalty: {}", e.getMessage()); }

        return new SaleResponse(savedSale.id(), savedSale.saleDate(), branch.name(), "Vendedor","Tef",
                totalBruto, totalDiscountGlobal, totalSurcharge, totalNeto, totalFinalCharged, null);
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

        // ESCENARIO A: FALTA STOCK (Split)
        if (currentRem < volumeNeeded) {
            log.info("    >> ESCENARIO A: Stock insuficiente. Realizando consumo split.");
            int volumeDeficit = volumeNeeded - currentRem;

            // 1. Consumir remanente actual
            log.info("       1. Consumiendo remanente de {}ml de botella ID {}", currentRem, decantBottle.id());
            registerInventoryMovement(decantBottle.id(), currentRem, "ML", userId);

            // 2. Reabastecer
            log.info("       2. Reabasteciendo desde botella sellada...");
            replenishDecantBottle(decantBottle, userId);
            Bottle refreshedBottle = bottleRepositoryPort.findById(decantBottle.id()).orElseThrow();

            // 3. Consumir déficit
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

        }
        // ESCENARIO B: SUFICIENTE STOCK
        else {
            log.info("    >> ESCENARIO B: Stock suficiente.");
            int nextRemaining = currentRem - volumeNeeded;

            bottleRepositoryPort.save(new Bottle(
                    decantBottle.id(), decantBottle.productId(), whId,
                    nextRemaining == 0 ? "DECANT_AGOTADA" : "DECANTADA",
                    decantBottle.barcode(), decantBottle.volumeMl(),
                    nextRemaining,
                    1
            ));

            registerInventoryMovement(decantBottle.id(), volumeNeeded, "ML", userId);
        }

        return new SaleItem(null, null, dp.id(), null, null, req.quantity(), req.price(),
                BigDecimal.ZERO, BigDecimal.ZERO, null, dp.volumeMl(), false, false, "NONE");
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

    private void updateLoyalty(Long clientId, BigDecimal amount) {
        // 1. Validamos que no sea el cliente genérico
        if (clientId == 1L) return;

        // 2. Buscamos el progreso actual (o creamos uno vacío si es nuevo)
        ClientLoyaltyProgress progress = loyaltyProgressRepositoryPort.findByClientId(clientId)
                .orElse(null);

        // Valores iniciales si es nuevo
        BigDecimal currentAccumulatedMoney = BigDecimal.ZERO;
        int currentPoints = 0;
        int currentTier = 1;

        // Si ya existe, recuperamos sus valores
        if (progress != null) {
            currentAccumulatedMoney = progress.accumulatedMoney();
            currentPoints = progress.pointsInTier();
            currentTier = progress.currentTier();
        }

        // -------------------------------------------------------
        // 3. LA LÓGICA: 1 SOL = 1 PUNTO
        // -------------------------------------------------------

        // A. Sumamos el dinero al histórico total
        BigDecimal totalMoney = currentAccumulatedMoney.add(amount);

        // B. Calculamos los puntos ganados en ESTA venta
        // .intValue() elimina los decimales.
        // Ej: 100.00 soles -> 100 puntos.
        // Ej: 50.90 soles  -> 50 puntos.
        int pointsEarned = amount.intValue();

        // C. Sumamos a los puntos que ya tenía
        int totalPoints = currentPoints + pointsEarned;

        // 4. Guardamos
        loyaltyProgressRepositoryPort.save(new ClientLoyaltyProgress(
                clientId,
                currentTier,
                totalPoints,
                totalMoney,
                LocalDateTime.now()
                // Si agregaste el purchase_count a esta entidad, añádelo aquí
        ));

        log.info("Loyalty actualizado Cliente {}: +{} Puntos (Total: {}). Dinero Total: {}",
                clientId, pointsEarned, totalPoints, totalMoney);
    }

    private void updateClientVipStatus(Long clientId) {
        if (clientId == 1L) return;

        Client client = clientRepositoryPort.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        int currentCounter = client.vipPurchaseCounter();
        int newCounter = currentCounter + 1;

        boolean isVip = client.isVip();
        LocalDateTime vipSince = client.vipSince();

        if (!isVip && newCounter >= 2) {
            isVip = true;
            vipSince = LocalDateTime.now(); // Marcamos la fecha de ascenso
            log.info("🌟 El Cliente {} ha alcanzado {} compras. ¡Ahora es VIP!", client.fullname(), newCounter);
        }

        clientRepositoryPort.save(new Client(
                client.id(),
                client.fullname(),
                client.dni(),
                client.email(),
                client.phone(),
                isVip,
                vipSince,
                newCounter,
                client.createdAt()
        ));
    }
}