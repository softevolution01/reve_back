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
import java.time.LocalDateTime;
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

    /**
     * Busca productos disponibles para contrato en el almacén de la sucursal.
     * Agrupa botellas del mismo producto para mostrar stock total.
     */
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

    /**
     * Crea un contrato, descuenta stock y registra el pago inicial en caja.
     */
    @Transactional(rollbackFor = Exception.class)
    public void createContract(ContractCreationRequest request) {
        // 1. Validaciones Iniciales
        Branch branchDomain = branchRepositoryPort.findById(request.branchId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        Long warehouseId = branchDomain.warehouseId();

        ProductEntity product = productRepository.findById(request.productId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // 2. Obtener Stock Disponible (Solo Botellas Selladas en el Almacén correcto)
        List<BottleEntity> stock = bottleRepository.findByProductId(request.productId());
        List<BottleEntity> availableBottles = stock.stream()
                .filter(b -> b.getWarehouse().getId().equals(warehouseId))
                .filter(b -> "SELLADA".equalsIgnoreCase(b.getStatus().getValue())) // Asumiendo que tu Enum tiene .getValue() o toString()
                .filter(b -> b.getQuantity() > 0)
                .toList();

        int quantityNeeded = request.quantity();
        int quantityFound = availableBottles.stream().mapToInt(BottleEntity::getQuantity).sum();

        if (quantityFound < quantityNeeded) {
            throw new RuntimeException("Stock insuficiente para contrato. Disponible: " + quantityFound);
        }

        // 3. Descontar Stock (CANTIDAD + VOLUMEN)
        int volumePerUnit = product.getVolumeProductsMl(); // Ej: 100ml

        for (BottleEntity bottle : availableBottles) {
            if (quantityNeeded <= 0) break;

            int currentQty = bottle.getQuantity();
            int take = Math.min(currentQty, quantityNeeded);

            // Cálculos de actualización
            int newQty = currentQty - take;
            int newVolumeMl = newQty * volumePerUnit;
            int newRemainingVolumeMl = newVolumeMl; // En selladas, el remanente es igual al total

            // Actualizar Entidad Botella
            bottle.setQuantity(newQty);
            bottle.setVolumeMl(newVolumeMl);
            bottle.setRemainingVolumeMl(newRemainingVolumeMl);

            // Si queda en 0, cambiar estado
            if (newQty == 0) {
                // Asegúrate de usar tu Enum correcto aquí
                bottle.setStatus(BottlesStatus.AGOTADA);
            }

            bottleRepository.save(bottle);

            // Registrar Movimiento de Inventario (Dominio)
            InventoryMovement invMov = new InventoryMovement(
                    null,
                    bottle.getId(),
                    take,
                    "EGRESO",
                    "UNIT",
                    "CONTRATO",
                    request.userId(),
                    LocalDateTime.now()
            );
            inventoryMovementRepositoryPort.save(invMov);

            quantityNeeded -= take;
        }

        // 4. Cálculos Financieros
        BigDecimal priceBase = BigDecimal.valueOf(product.getPrice()).multiply(BigDecimal.valueOf(request.quantity()));
        BigDecimal finalPrice = priceBase.subtract(request.discount());
        BigDecimal pendingBalance = finalPrice.subtract(request.advancePayment());

        // 5. Crear y Guardar Contrato (JPA)
        ContractEntity contract = ContractEntity.builder()
                .client(ClientEntity.builder().id(request.clientId()).build())
                .user(UserEntity.builder().id(request.userId()).build())
                .branch(BranchEntity.builder().id(request.branchId()).build())
                .product(product)
                .quantity(request.quantity())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .priceBase(priceBase)
                .discount(request.discount())
                .finalPrice(finalPrice)
                .advancePayment(request.advancePayment())
                .pendingBalance(pendingBalance)
                .status("PENDIENTE")
                .createdAt(LocalDateTime.now())
                .build();

        ContractEntity savedContract = contractRepository.save(contract);
        PaymentMethodEntity pm = sprigDataPaymentMethodRepository.findById(request.paymentMethodId())
                .orElseThrow(() -> new RuntimeException("Método no encontrado"));

        // 6. REGISTRAR CAJA (ADELANTO) - USANDO EL SERVICIO CENTRALIZADO
        if (request.advancePayment().compareTo(BigDecimal.ZERO) > 0) {
            manageCashSessionUseCase.registerMovement(
                    request.branchId(),
                    request.userId(),
                    "VENTA", // O "INGRESO CONTRATO"
                    request.advancePayment(),
                    "Adelanto Contrato #" + savedContract.getId(),
                    pm.getName()
            );
        }
    }

    /**
     * Lista todos los contratos paginados.
     */
    @Transactional(readOnly = true)
    public Page<ContractListResponse> getAllContracts(int page, int size) {
        return contractRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(c -> new ContractListResponse(
                        c.getId(),
                        c.getClient().getFullname(),
                        c.getProduct().getBrand() + " " + c.getProduct().getLine(),
                        c.getQuantity(),
                        c.getStartDate(),
                        c.getEndDate(),
                        c.getPriceBase(),
                        c.getFinalPrice(),
                        c.getAdvancePayment(),
                        c.getPendingBalance(),
                        c.getStatus()
                ));
    }

    /**
     * Finaliza un contrato y registra el cobro del saldo restante en caja.
     */
    @Transactional
    public void finalizeContract(Long contractId, Long userId, Long paymentMethodId) {

        ContractEntity contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));

        // 2. BUSCAMOS EL MÉTODO DE PAGO
        PaymentMethodEntity pm = sprigDataPaymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new RuntimeException("Método de pago no encontrado"));

        if (!"PENDIENTE".equalsIgnoreCase(contract.getStatus())) {
            throw new RuntimeException("El contrato no está pendiente.");
        }

        // 3. REGISTRAR CAJA SI HAY SALDO PENDIENTE
        if (contract.getPendingBalance().compareTo(BigDecimal.ZERO) > 0) {

            manageCashSessionUseCase.registerMovement(
                    contract.getBranch().getId(),
                    userId,
                    "VENTA",
                    contract.getPendingBalance(),
                    "FINALIZACION CONTRATO #" + contract.getId() + " (Saldo)",
                    pm.getName()
            );
        }

        contract.setStatus("FINALIZADO");
        contract.setPendingBalance(BigDecimal.ZERO); // Es buena práctica dejar el saldo en 0
        contractRepository.save(contract);
    }
}