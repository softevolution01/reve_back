package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.out.*;
import reve_back.domain.model.Branch;
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
    private final CashMovementRepositoryPort cashMovementRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final BranchRepositoryPort branchRepositoryPort;

    @Transactional(readOnly = true)
    public List<ProductContractLookupResponse> findProductsForContract(Long branchId, String query) {
        // Obtenemos el objeto de Dominio (Record Branch)
        Branch branch = branchRepositoryPort.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        List<BottleEntity> bottles = bottleRepository.findActiveByProductNameLike(query, PageRequest.of(0, 50));

        return bottles.stream()
                // CORRECCIÓN 1: Usamos el método del Record 'warehouseId()' directamente
                .filter(b -> b.getWarehouse().getId().equals(branch.warehouseId()))
                .filter(b -> b.getQuantity() > 0)
                .map(b -> new ProductContractLookupResponse(
                        b.getProduct().getId(),
                        b.getProduct().getBrand() + " " + b.getProduct().getLine() + " " + b.getProduct().getConcentration(),
                        new BigDecimal(b.getProduct().getPrice()),
                        b.getQuantity()
                ))
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
        // 1. Referencias y Validaciones Iniciales
        BranchEntity branchRef = BranchEntity.builder().id(request.branchId()).build();

        var branchDomain = branchRepositoryPort.findById(request.branchId()).orElseThrow();
        Long warehouseId = branchDomain.warehouseId();

        ProductEntity product = productRepository.findById(request.productId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // 2. Obtener Stock Disponible (Solo Botellas Selladas)
        List<BottleEntity> stock = bottleRepository.findByProductId(request.productId());
        List<BottleEntity> availableBottles = stock.stream()
                .filter(b -> b.getWarehouse().getId().equals(warehouseId))
                .filter(b -> "SELLADA".equalsIgnoreCase(b.getStatus().getValue()))
                .filter(b -> b.getQuantity() > 0)
                .toList();

        int quantityNeeded = request.quantity();
        int quantityFound = availableBottles.stream().mapToInt(BottleEntity::getQuantity).sum();

        if (quantityFound < quantityNeeded) {
            throw new RuntimeException("Stock insuficiente para contrato. Disponible: " + quantityFound);
        }

        // 3. Descontar Stock (CANTIDAD + VOLUMEN + VOLUMEN RESTANTE)
        // Obtenemos el volumen unitario del producto (ej: 100ml)
        int volumePerUnit = product.getVolumeProductsMl();

        for (BottleEntity bottle : availableBottles) {
            if (quantityNeeded <= 0) break;

            int currentQty = bottle.getQuantity();
            int take = Math.min(currentQty, quantityNeeded);

            // Calculamos la nueva cantidad
            int newQty = currentQty - take;

            // NUEVO: Recalculamos volúmenes basados en la nueva cantidad
            // Si quedan 4 botellas de 100ml, el volumen debe ser 400ml
            int newVolumeMl = newQty * volumePerUnit;
            int newRemainingVolumeMl = newVolumeMl; // En botellas selladas, remanente = total

            // Actualizamos la Entidad
            bottle.setQuantity(newQty);
            bottle.setVolumeMl(newVolumeMl);                  // <--- Actualización Clave
            bottle.setRemainingVolumeMl(newRemainingVolumeMl);// <--- Actualización Clave

            // Verificar si se agotó este lote/botella
            if (newQty == 0) {
                bottle.setStatus(reve_back.domain.model.BottlesStatus.AGOTADA);
                // Por seguridad, si es 0 cantidad, forzamos volumen a 0 (aunque el cálculo arriba ya lo hace)
                bottle.setVolumeMl(0);
                bottle.setRemainingVolumeMl(0);
            }

            bottleRepository.save(bottle);

            // Registrar el movimiento en el historial
            reve_back.domain.model.InventoryMovement invMov = new reve_back.domain.model.InventoryMovement(
                    null, bottle.getId(), take, "EGRESO", "UNIT", "CONTRATO", request.userId(), LocalDateTime.now()
            );
            inventoryMovementRepositoryPort.save(invMov);

            quantityNeeded -= take;
        }

        // 4. Cálculos Financieros del Contrato
        BigDecimal priceBase = new BigDecimal(product.getPrice()).multiply(new BigDecimal(request.quantity()));
        BigDecimal finalPrice = priceBase.subtract(request.discount());
        BigDecimal pendingBalance = finalPrice.subtract(request.advancePayment());

        // 5. Guardar Contrato
        ContractEntity contract = ContractEntity.builder()
                .client(ClientEntity.builder().id(request.clientId()).build())
                .user(UserEntity.builder().id(request.userId()).build())
                .branch(branchRef)
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
                .build();

        ContractEntity savedContract = contractRepository.save(contract);

        // 6. Registrar Movimiento de Caja (Si hubo adelanto)
        if (request.advancePayment().compareTo(BigDecimal.ZERO) > 0) {
            reve_back.domain.model.CashMovement cashMov = new reve_back.domain.model.CashMovement(
                    null, request.branchId(), request.advancePayment(), "INGRESO",
                    "ADELANTO CONTRATO #" + savedContract.getId(), request.userId(), null, LocalDateTime.now()
            );
            cashMovementRepositoryPort.save(cashMov);
        }
    }

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

    @Transactional
    public void finalizeContract(Long contractId, Long userId) {
        ContractEntity contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));

        if (!"PENDIENTE".equalsIgnoreCase(contract.getStatus())) {
            throw new RuntimeException("El contrato no está pendiente.");
        }

        if (contract.getPendingBalance().compareTo(BigDecimal.ZERO) > 0) {
            reve_back.domain.model.CashMovement cashMov = new reve_back.domain.model.CashMovement(
                    null,
                    contract.getBranch().getId(),
                    contract.getPendingBalance(),
                    "INGRESO",
                    "FINALIZACION CONTRATO #" + contract.getId(),
                    userId,
                    null,
                    LocalDateTime.now()
            );
            cashMovementRepositoryPort.save(cashMov);
        }

        contract.setAdvancePayment(contract.getFinalPrice());

        contract.setPendingBalance(BigDecimal.ZERO);

        contract.setStatus("FINALIZADO");
        contractRepository.save(contract);
    }
}