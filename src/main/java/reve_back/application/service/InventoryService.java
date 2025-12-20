package reve_back.application.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reve_back.application.ports.in.CreateSaleUseCase;
import reve_back.application.ports.in.InventoryMovementUseCase;
import reve_back.application.ports.out.BottleRepositoryPort;
import reve_back.application.ports.out.CashMovementRepositoryPort;
import reve_back.application.ports.out.ProductRepositoryPort;
import reve_back.application.ports.out.SalesRepositoryPort;
import reve_back.domain.model.*;
import reve_back.infrastructure.persistence.entity.InventoryMovementEntity;
import reve_back.infrastructure.persistence.enums.global.MovementUnit;
import reve_back.infrastructure.persistence.jpa.SpringDataMovementRepository;
import reve_back.infrastructure.persistence.mapper.PersistenceMapper;
import reve_back.infrastructure.web.dto.QuickMovementRequest;

@Service
@RequiredArgsConstructor
public class InventoryService implements InventoryMovementUseCase {

    private final BottleRepositoryPort bottleRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final SpringDataMovementRepository movementRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processMovement(QuickMovementRequest request) {

        String typeStr = request.type().toUpperCase().trim();
        String unit = request.unit().toUpperCase().trim();
        MovementType movementType;
        MovementUnit movementUnit;

        try {
            movementType = MovementType.valueOf(typeStr);
            movementUnit = MovementUnit.valueOf(unit);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error: Tipo inválido. Use INGRESO o EGRESO.");
        }

        Bottle bottle = bottleRepositoryPort.findById(request.bottleId())
                .orElseThrow(() -> new RuntimeException("Botella no encontrada"));

        Product product = productRepositoryPort.findById(bottle.productId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        String currentStatus = bottle.status() != null ? bottle.status().toUpperCase() : "AGOTADA";

        if (movementType == MovementType.INGRESO && "UNIT".equals(unit)) {
            if (currentStatus.contains("DECANT")) {
                throw new RuntimeException("No puedes mezclar Botellas Selladas en una fila de Decants.");
            }
        }

        int newQty = bottle.quantity();
        int newVolumeML = bottle.volumeMl();
        int newRemainingVolumeMl = bottle.remainingVolumeMl();
        int volumenPorUnidad = product.volumeProductsMl();

        if ("UNIT".equals(unit)) {
            // 1. Primero calculamos SOLO la nueva cantidad física
            if (movementType == MovementType.INGRESO) {
                newQty = bottle.quantity() + request.quantity();
            } else { // EGRESO
                newQty = bottle.quantity() - request.quantity();

                // Validación de seguridad
                if (newQty < 0) {
                    throw new RuntimeException("Stock insuficiente: No puedes retirar " + request.quantity() + " botellas si solo tienes " + bottle.quantity());
                }
            }

            // 2. AHORA RECALCULAMOS EL VOLUMEN (Aquí está la corrección que pediste)
            // Esto garantiza la consistencia exacta:
            // 2 botellas = 200ml.
            // Si quitas 1 -> Queda 1 botella = 100ml.
            newVolumeML = newQty * volumenPorUnidad;

            // En botellas SELLADAS, el "Remaining" siempre es igual al Total
            newRemainingVolumeMl = newVolumeML;
        }
        else if ("ML".equals(unit)) {
            // ... (La lógica de ML / Decants se mantiene igual) ...
            if (movementType == MovementType.INGRESO) {
                newRemainingVolumeMl += request.quantity();
            } else {
                newRemainingVolumeMl -= request.quantity();
                if (newRemainingVolumeMl < 0) throw new RuntimeException("Mililitros insuficientes.");
            }
            // Nota: En ML no tocamos newQty ni newVolumeML total, solo el remaining
        }

        String newStatus = currentStatus;

        if ("UNIT".equals(unit)) {
            if (newQty == 0) {
                newStatus = "AGOTADA";
            } else {
                newStatus = "SELLADA";
            }
        }
        else if ("ML".equals(unit)) {
            if (newRemainingVolumeMl == 0) {
                newStatus = "DECANT_AGOTADA";
            } else {
                newStatus = "DECANTADA";
            }
        }

        Bottle updatedBottle = new Bottle(
                bottle.id(),
                bottle.productId(),
                bottle.warehouseId(),
                newStatus,
                bottle.barcode(),
                newVolumeML,
                newRemainingVolumeMl,
                newQty
        );
        bottleRepositoryPort.save(updatedBottle);

        InventoryMovementEntity history = new InventoryMovementEntity();
        history.setBottleId(bottle.id());
        history.setQuantity(request.quantity());
        history.setType(movementType);
        history.setUnit(movementUnit);
        history.setReason(request.reason().toUpperCase().trim());
        history.setUserId(request.userId());

        movementRepository.save(history);
    }



}
