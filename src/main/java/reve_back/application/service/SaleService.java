package reve_back.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.in.CreateSaleUseCase;
import reve_back.application.ports.in.ManageCashSessionUseCase;
import reve_back.application.ports.out.*;
import reve_back.domain.model.*;
import reve_back.infrastructure.persistence.entity.ClientLoyaltyProgressEntity;
import reve_back.infrastructure.persistence.entity.PromotionEntity;
import reve_back.infrastructure.persistence.enums.global.PromotionRuleType;
import reve_back.infrastructure.persistence.jpa.SpringDataPromotionRepository;
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
    private final PromotionRepositoryPort promotionRepositoryPort;
    private final LoyaltyProgressRepositoryPort loyaltyProgressRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final PaymentMethodsRepositoryPort paymentMethodRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final LoyaltyTiersRepositoryPort loyaltyTiersRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    private final CashSessionRepositoryPort cashSessionPort;
    private final ManageCashSessionUseCase manageCashSessionUseCase;

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
        String clientName = client.fullname();
        String clientDNI = client.dni();

        Branch branch = branchRepositoryPort.findById(request.branchId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));
        Long warehouseId = branch.warehouseId();

        // 1. VALIDACIÓN DE CAJA (CRÍTICO)
        var sessionOpt = cashSessionPort.findOpenSessionByWarehouse(warehouseId);
        if (sessionOpt.isEmpty()) {
            throw new RuntimeException("⛔ NO SE PUEDE VENDER: La caja del almacén está CERRADA. Por favor, abra turno primero.");
        }
        Long activeSessionId = sessionOpt.get().getId();

        // 2. Procesamiento de Pagos
        List<SalePayment> salePayments = new ArrayList<>();
        BigDecimal totalSurcharge = BigDecimal.ZERO;
        Set<String> methodNames = new HashSet<>();

        Map<String, BigDecimal> paymentsToRegister = new HashMap<>();

        for (PaymentRequest pReq : request.payments()) {
            PaymentMethod pm = paymentMethodRepositoryPort.findById(pReq.paymentMethodId())
                    .orElseThrow(() -> new RuntimeException("Método de pago no encontrado"));

            String methodName = pm.name().toUpperCase();
            methodNames.add(methodName);

            BigDecimal surcharge = BigDecimal.ZERO;
            if (pm.surchargePercentage() != null && pm.surchargePercentage().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal divisor = new BigDecimal("100").add(pm.surchargePercentage());
                surcharge = pReq.amount()
                        .multiply(pm.surchargePercentage())
                        .divide(divisor, 2, RoundingMode.HALF_UP);

                totalSurcharge = totalSurcharge.add(surcharge);
            }

            salePayments.add(new SalePayment(null, pReq.paymentMethodId(), pm.name(), pReq.amount(), surcharge));

            BigDecimal totalLineAmount = pReq.amount();
            paymentsToRegister.merge(methodName, totalLineAmount, BigDecimal::add);
        }
        String paymentMethodName = (methodNames.size() > 1) ? "MIXTO" : methodNames.iterator().next();

        // 3. Procesamiento de Items Inicial
        List<SaleItem> tempItems = new ArrayList<>();
        BigDecimal totalManualDiscount = BigDecimal.ZERO;

        for (SaleItemRequest itemReq : request.items()) {
            SaleItem domainItem = processStockLogic(itemReq, warehouseId, request.userId());

            BigDecimal itemManualDiscount = itemReq.manualDiscount() != null ? itemReq.manualDiscount() : BigDecimal.ZERO;
            totalManualDiscount = totalManualDiscount.add(itemManualDiscount);

            tempItems.add(new SaleItem(
                    null,
                    domainItem.productId(),
                    domainItem.decantPriceId(),
                    domainItem.productName(),
                    domainItem.productBrand(),
                    domainItem.quantity(), domainItem.unitPrice(),
                    BigDecimal.ZERO,
                    itemManualDiscount,
                    BigDecimal.ZERO,
                    domainItem.volumeMlPerUnit(),
                    itemReq.blockedPromo() != null ? itemReq.blockedPromo() : false,
                    itemReq.forcePromo() != null ? itemReq.forcePromo() : false,
                    "NONE"
            ));
        }

        // =================================================================================
        // 4. LÓGICA DINÁMICA DE PROMOCIONES (Distribución Exacta por Línea)
        // =================================================================================
        if (request.promotionId() != null) {
            // Nota: Usa el nombre de tu repositorio de promociones inyectado aquí
            Promotion promotion = promotionRepositoryPort.findActivePromotionById(request.promotionId())
                    .orElseThrow(() -> new RuntimeException("Promoción no encontrada o inactiva"));

            String strategyCode = promotion.strategyCode();
            int buyQty = extractRuleValue(promotion, PromotionRuleType.CONFIG_BUY_QUANTITY, 3);
            int payQty = extractRuleValue(promotion, PromotionRuleType.CONFIG_PAY_QUANTITY, 2);
            BigDecimal extraDiscount = extractBigDecimalRule(promotion, PromotionRuleType.EXTRA_DISCOUNT_PERCENT, new BigDecimal("50.00"));
            int freeQty = buyQty - payQty;

            // Clase interna para rastrear la unidad exacta y su índice en tempItems
            class PromoUnit {
                int originalIndex;
                BigDecimal price;
                PromoUnit(int index, BigDecimal price) { this.originalIndex = index; this.price = price; }
            }

            List<PromoUnit> promoUnits = new ArrayList<>();
            for (int i = 0; i < tempItems.size(); i++) {
                SaleItem item = tempItems.get(i);
                if (item.isPromoLocked() || item.isPromoForced()) {
                    // Desglosamos por cantidad (Ej: Si lleva 2 de 10ml, creamos 2 unidades separadas)
                    for (int q = 0; q < item.quantity(); q++) {
                        promoUnits.add(new PromoUnit(i, item.unitPrice()));
                    }
                }
            }

            // Ordenamos de MAYOR a MENOR precio
            promoUnits.sort((a, b) -> b.price.compareTo(a.price));
            Deque<PromoUnit> deque = new ArrayDeque<>(promoUnits);

            // Acumulador de descuentos exactos por línea
            Map<Integer, BigDecimal> exactLineDiscounts = new HashMap<>();

            while (deque.size() >= buyQty) {
                // A) Cobrar los M más caros
                for (int i = 0; i < payQty; i++) {
                    deque.pollFirst(); // Se saca y se ignora (no hay descuento)
                }

                // B) Regalar el MÁS BARATO (GRATIS)
                for (int i = 0; i < freeQty; i++) {
                    PromoUnit freeUnit = deque.pollLast();
                    if (freeUnit != null) {
                        exactLineDiscounts.merge(freeUnit.originalIndex, freeUnit.price, BigDecimal::add);
                    }
                }

                // C) El 4to al 50%
                if ("SANDWICH_PLUS_EXTRA".equals(strategyCode) && !deque.isEmpty()) {
                    PromoUnit extraUnit = deque.pollLast();
                    if (extraUnit != null) {
                        BigDecimal discountAmt = extraUnit.price.multiply(extraDiscount)
                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                        exactLineDiscounts.merge(extraUnit.originalIndex, discountAmt, BigDecimal::add);
                    }
                }
            }

            // Aplicar el descuento mapeado exactamente a la línea que le corresponde
            for (int i = 0; i < tempItems.size(); i++) {
                SaleItem currentItem = tempItems.get(i);
                BigDecimal lineDiscount = exactLineDiscounts.getOrDefault(i, BigDecimal.ZERO);

                // Reemplazamos el item en la lista con su descuento real asignado
                if (lineDiscount.compareTo(BigDecimal.ZERO) > 0 || currentItem.isPromoLocked()) {
                    tempItems.set(i, new SaleItem(
                            currentItem.id(), currentItem.productId(), currentItem.decantPriceId(),
                            currentItem.productName(), currentItem.productBrand(),
                            currentItem.quantity(), currentItem.unitPrice(),
                            lineDiscount, // <--- EL DESCUENTO EXACTO DE LA LÍNEA VA AQUÍ
                            currentItem.manualDiscount(),
                            BigDecimal.ZERO,
                            currentItem.volumeMlPerUnit(),
                            currentItem.isPromoLocked(),
                            currentItem.isPromoForced(),
                            strategyCode
                    ));
                }
            }
        }
        // =================================================================================

        // 5. Cálculo de Totales Finales
        List<SaleItem> finalSaleItems = new ArrayList<>();
        BigDecimal totalBruto = BigDecimal.ZERO;
        BigDecimal totalSystemDiscountApplied = BigDecimal.ZERO;
        BigDecimal totalLoyaltyAmount = BigDecimal.ZERO;

        for (SaleItem item : tempItems) {
            BigDecimal lineGrossTotal = item.unitPrice().multiply(new BigDecimal(item.quantity()));

            // Al gross total le restamos el descuento manual y el SystemDiscount EXACTO de esa línea
            BigDecimal lineNetTotal = lineGrossTotal
                    .subtract(item.manualDiscount())
                    .subtract(item.systemDiscount());

            finalSaleItems.add(new SaleItem(
                    item.id(), item.productId(), item.decantPriceId(), item.productName(), item.productBrand(),
                    item.quantity(), item.unitPrice(), item.systemDiscount(), item.manualDiscount(),
                    lineNetTotal,
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
        Sale saleToSave = new Sale(
                null,
                LocalDateTime.now(),
                branch.id(),
                request.userId(),
                finalClientId,
                activeSessionId,
                clientName,
                totalBruto,
                totalDiscountGlobal,
                new BigDecimal("0.18"),
                totalSurcharge,
                totalFinalCharged,
                paymentMethodName,
                finalSaleItems,
                salePayments
        );

        Sale savedSale = salesRepositoryPort.save(saleToSave);
        log.info("✅ Venta Guardada con ID: {}", savedSale.id());

        paymentsToRegister.forEach((method, amount) -> {
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                manageCashSessionUseCase.registerMovement(
                        branch.id(),
                        request.userId(),
                        "VENTA",
                        amount,
                        "Venta " + method + " #" + savedSale.id(),
                        method,
                        savedSale.id(),
                        null
                );
            }
        });

        // 8. Post-Venta (Lealtad)
        try {
            boolean triggerVipReset = updateClientVipStatus(finalClientId);
            updateLoyalty(finalClientId, totalLoyaltyAmount, triggerVipReset);
        } catch (Exception e) {
            log.error("Error Post-Venta: {}", e.getMessage());
        }

        // 9. Construcción de Respuesta
        List<SaleItemResponse> itemsResponse = finalSaleItems.stream()
                .map(item -> {
                    String typeDisplay = (item.volumeMlPerUnit() != null) ? item.volumeMlPerUnit().intValue() + "ml" : "Botella";
                    String safeProductName = item.productName() != null ? item.productName() : "SIN NOMBRE";
                    String prefix = (item.decantPriceId() == null) ? "BT " : "DC ";

                    // Solo agregamos el sufijo PROMO si realmente tuvo un descuento automático asignado
                    String promoSuffix = item.systemDiscount().compareTo(BigDecimal.ZERO) > 0 ? " (PROMO)" : "";
                    String finalDisplayName = prefix + " " + safeProductName + promoSuffix;

                    BigDecimal totalDiscLine = (item.manualDiscount() == null ? BigDecimal.ZERO : item.manualDiscount())
                            .add(item.systemDiscount() == null ? BigDecimal.ZERO : item.systemDiscount());

                    return new SaleItemResponse(
                            finalDisplayName,
                            typeDisplay,
                            item.quantity(),
                            item.unitPrice(),
                            item.finalSubtotal(), // <--- Esto es lo que imprime tu boleta en "TOTAL"
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
                paymentMethodName,
                itemsResponse
        );
    }

    private SaleItem processStockLogic(SaleItemRequest req, Long whId, Long userId) {
        if ("BOTELLA".equalsIgnoreCase(req.tipoVendible())) {
            log.info("Lógica Directa: Buscando botella SELLADA basada en referencia ID: {}", req.idInventario());

            Product product = productRepositoryPort.findById(req.idInventario())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado para recalcular stock."));

            Long productId = req.idInventario();

            log.info("🛒 Venta Iniciada. Producto ID: {} (Ref enviada: {}) Almacen ID: {}", productId, req.idInventario(), whId);

            Bottle targetBottle = bottleRepositoryPort.findSellableBottle(productId, whId)
                    .orElseThrow(() -> new RuntimeException(
                            "No hay stock SELLADO disponible para el producto ID " + productId + " en esta sede."
                    ));

            log.info("✅ Botella Seleccionada para Descuento: ID {} | Barcode {} | Stock Actual: {}",
                    targetBottle.id(), targetBottle.barcode(), targetBottle.quantity());


            if (targetBottle.quantity() < req.quantity()) {
                throw new RuntimeException(String.format(
                        "Stock Insuficiente. Disponible: %d, Solicitado: %d",
                        targetBottle.quantity(), req.quantity()
                ));
            }

            int newQuantity = targetBottle.quantity() - req.quantity();
            boolean isDepleted = newQuantity <= 0;
            int newVolumen = targetBottle.volumeMl() - product.volumeProductsMl()* req.quantity();


            Bottle updatedBottle = new Bottle(
                    targetBottle.id(),
                    targetBottle.productId(),
                    whId,
                    isDepleted ? "AGOTADA" : "SELLADA",
                    targetBottle.barcode(),
                    newVolumen,
                    isDepleted ? 0 : newVolumen,
                    newQuantity
            );

            bottleRepositoryPort.save(updatedBottle);

            registerInventoryMovement(targetBottle.id(), req.quantity(), "UNIT", userId);

            return new SaleItem(null, productId, null, product.brand()+ " "+product.line(), null,
                    req.quantity(), req.price(), BigDecimal.ZERO, BigDecimal.ZERO,
                    null, product.volumeProductsMl(), false, false, "NONE");
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

        return new SaleItem(null, dp.productId(), dp.id(), product.brand() +" "+ product.line(), null, req.quantity(), req.price(),
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

    // MÉTODOS EXTRACTORES SIMPLIFICADOS PARA TU DOMINIO

    private int extractRuleValue(Promotion promotion, PromotionRuleType type, int defaultValue) {
        if (promotion == null || promotion.rules() == null) return defaultValue;

        return promotion.rules().stream()
                .filter(r -> r.ruleType() == type) // Cambiar a getRuleType() si no compila
                .map(r -> {
                    if (r.discountValue() != null) { // Cambiar a getDiscountValue() si no compila
                        return r.discountValue().intValue();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(defaultValue);
    }

    private BigDecimal extractBigDecimalRule(Promotion promotion, PromotionRuleType type, BigDecimal defaultValue) {
        if (promotion == null || promotion.rules() == null) return defaultValue;

        return promotion.rules().stream()
                .filter(r -> r.ruleType() == type) // Cambiar a getRuleType() si no compila
                .map(r -> r.discountValue())       // Cambiar a getDiscountValue() si no compila
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(defaultValue);
    }
}