package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.in.ManageCashSessionUseCase;
import reve_back.application.ports.out.*;
import reve_back.domain.model.*;
import reve_back.infrastructure.persistence.entity.*;
import reve_back.infrastructure.persistence.jpa.*;
import reve_back.infrastructure.web.dto.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final SpringDataContractRepository contractRepository;
    private final SpringDataBottleRepository bottleRepository;
    private final SpringDataProductRepository productRepository;

    // Puertos (Arquitectura Hexagonal)
    private final ManageCashSessionUseCase manageCashSessionUseCase; // <--- CLAVE PARA LA CAJA
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final BranchRepositoryPort branchRepositoryPort;
    private final SprigDataPaymentMethodRepository sprigDataPaymentMethodRepository;

    @Transactional(readOnly = true)
    public List<ProductContractLookupResponse> findProductsForContract(Long branchId, String query) {
        // 1. Obtener datos de la Sede (Dominio)
        Branch branch = branchRepositoryPort.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        // 2. Buscar botellas por nombre de producto (JPA)
        List<BottleEntity> bottles = bottleRepository.findActiveByProductNameLike(query, PageRequest.of(0, 50));

        // 3. Filtrar por Almacén y Agrupar
        return bottles.stream()
                // Validamos que la botella esté en el almacén de la sucursal
                .filter(b -> b.getWarehouse().getId().equals(branch.warehouseId()))
                .filter(b -> b.getQuantity() > 0)
                .map(b -> new ProductContractLookupResponse(
                        b.getProduct().getId(),
                        b.getProduct().getBrand() + " " + b.getProduct().getLine() + " " + b.getProduct().getConcentration(),
                        BigDecimal.valueOf(b.getProduct().getPrice()),
                        b.getQuantity()
                ))
                // Agrupamos por ID de producto sumando el stock
                .collect(Collectors.toMap(
                        ProductContractLookupResponse::productId,
                        p -> p,
                        (p1, p2) -> new ProductContractLookupResponse(
                                p1.productId(), p1.fullName(), p1.price(), p1.currentStock() + p2.currentStock())
                ))
                .values().stream().toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void createContract(ContractCreationRequest request) {
        Branch branchDomain = branchRepositoryPort.findById(request.branchId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));
        Long warehouseId = branchDomain.warehouseId();

        // 2. Preparar el Contrato Padre (Aún sin guardar items)
        ContractEntity contract = ContractEntity.builder()
                .client(ClientEntity.builder().id(request.clientId()).build())
                .user(UserEntity.builder().id(request.userId()).build())
                .branch(BranchEntity.builder().id(request.branchId()).build())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status("PENDIENTE")
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>()) // Inicializamos lista
                .build();

        BigDecimal totalBasePrice = BigDecimal.ZERO;

        // 3. BUCLE: Procesar cada producto (Inventario + Detalle)
        for (ContractItemRequest itemReq : request.items()) {

            ProductEntity product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new RuntimeException("Producto " + itemReq.productId() + " no encontrado"));

            // --- LÓGICA DE STOCK (Tu código original adaptado al bucle) ---
            List<BottleEntity> stock = bottleRepository.findByProductId(itemReq.productId());
            List<BottleEntity> availableBottles = stock.stream()
                    .filter(b -> b.getWarehouse().getId().equals(warehouseId))
                    .filter(b -> "SELLADA".equalsIgnoreCase(b.getStatus().getValue()))
                    .filter(b -> b.getQuantity() > 0)
                    .toList();

            int quantityNeeded = itemReq.quantity();
            int quantityFound = availableBottles.stream().mapToInt(BottleEntity::getQuantity).sum();

            if (quantityFound < quantityNeeded) {
                throw new RuntimeException("Stock insuficiente para " + product.getBrand() + ". Requerido: " + quantityNeeded + ", Disponible: " + quantityFound);
            }

            // Descuento de botellas
            int volumePerUnit = product.getVolumeProductsMl();
            for (BottleEntity bottle : availableBottles) {
                if (quantityNeeded <= 0) break;
                int currentQty = bottle.getQuantity();
                int take = Math.min(currentQty, quantityNeeded);

                bottle.setQuantity(currentQty - take);
                bottle.setVolumeMl((currentQty - take) * volumePerUnit);
                bottle.setRemainingVolumeMl(bottle.getVolumeMl());
                if (bottle.getQuantity() == 0) bottle.setStatus(BottlesStatus.AGOTADA);

                bottleRepository.save(bottle);

                // Movimiento de inventario
                InventoryMovement invMov = new InventoryMovement(null, bottle.getId(), take, "EGRESO", "UNIT", "CONTRATO", request.userId(), LocalDateTime.now());
                inventoryMovementRepositoryPort.save(invMov);

                quantityNeeded -= take;
            }

            BigDecimal itemPrice = BigDecimal.valueOf(product.getPrice());
            BigDecimal itemSubtotal = itemPrice.multiply(BigDecimal.valueOf(itemReq.quantity()));

            totalBasePrice = totalBasePrice.add(itemSubtotal);
            ContractItemEntity detail = ContractItemEntity.builder()
                    .contract(contract)
                    .product(product)
                    .quantity(itemReq.quantity())
                    .unitPrice(itemPrice)
                    .subtotal(itemSubtotal)
                    .build();

            contract.getItems().add(detail);
        }

        BigDecimal finalPrice = totalBasePrice.subtract(request.discount());
        BigDecimal pendingBalance = finalPrice.subtract(request.advancePayment());

        contract.setPriceBase(totalBasePrice);
        contract.setDiscount(request.discount());
        contract.setFinalPrice(finalPrice);
        contract.setAdvancePayment(request.advancePayment());
        contract.setPendingBalance(pendingBalance);

        ContractEntity savedContract = contractRepository.save(contract);

        if (request.advancePayment().compareTo(BigDecimal.ZERO) > 0) {

            PaymentMethodEntity pm = sprigDataPaymentMethodRepository.findById(request.paymentMethodId())
                    .orElseThrow(() -> new RuntimeException("Método no encontrado"));

            BigDecimal debtAmount = request.advancePayment();

            BigDecimal cashAmount = debtAmount;

            if (pm.getSurchargePercentage() != null && pm.getSurchargePercentage().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal surcharge = debtAmount
                        .multiply(pm.getSurchargePercentage())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                cashAmount = debtAmount.add(surcharge);
            }

            manageCashSessionUseCase.registerMovement(
                    request.branchId(),
                    request.userId(),
                    "VENTA",
                    cashAmount,
                    "Adelanto Contrato #" + savedContract.getId() + " (" + pm.getName() + ")",
                    pm.getName(),
                    null,
                    savedContract.getId()

            );
        }
    }

    @Transactional(readOnly = true)
    public Page<ContractListResponse> getAllContracts(int page, int size) {
        return contractRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(c -> {

                    String productsSummary = c.getItems().stream()
                            .map(item -> item.getProduct().getBrand())
                            .collect(Collectors.joining(", "));

                    if (productsSummary.length() > 50) {
                        productsSummary = productsSummary.substring(0, 47) + "...";
                    }

                    Integer totalQuantity = c.getItems().stream()
                            .mapToInt(ContractItemEntity::getQuantity)
                            .sum();

                    return new ContractListResponse(
                            c.getId(),
                            c.getClient().getFullname(),
                            productsSummary,
                            totalQuantity,
                            c.getStartDate(),
                            c.getEndDate(),
                            c.getPriceBase(),
                            c.getFinalPrice(),
                            c.getAdvancePayment(),
                            c.getPendingBalance(),
                            c.getStatus()
                    );
                });
    }

    /**
     * Finaliza un contrato y registra el cobro del saldo restante en caja.
     */
    @Transactional
    public void finalizeContract(Long contractId, Long userId, Long paymentMethodId) {

        ContractEntity contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));

        PaymentMethodEntity pm = sprigDataPaymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new RuntimeException("Método de pago no encontrado"));

        if (!"PENDIENTE".equalsIgnoreCase(contract.getStatus())) {
            throw new RuntimeException("El contrato no está pendiente.");
        }

        BigDecimal debtBalance = contract.getPendingBalance(); // Ej: 200.00

        if (debtBalance.compareTo(BigDecimal.ZERO) > 0) {

            // A. Calculamos el monto real a cobrar (BASE + RECARGO)
            BigDecimal cashAmount = debtBalance;

            if (pm.getSurchargePercentage() != null && pm.getSurchargePercentage().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal surcharge = debtBalance
                        .multiply(pm.getSurchargePercentage())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                cashAmount = debtBalance.add(surcharge); // Ej: 208.00
            }

            // B. Registramos en caja los 208.00
            manageCashSessionUseCase.registerMovement(
                    contract.getBranch().getId(),
                    userId,
                    "VENTA",
                    cashAmount, // <--- MONTO CON RECARGO
                    "FINALIZACION CONTRATO #" + contract.getId() + " (Saldo)",
                    pm.getName(),
                    null,
                    contractId
            );
        }

        contract.setStatus("FINALIZADO");
        contract.setPendingBalance(BigDecimal.ZERO); // Es buena práctica dejar el saldo en 0
        contractRepository.save(contract);
    }
}