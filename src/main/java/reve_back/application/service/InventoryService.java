package reve_back.application.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reve_back.application.ports.in.InventoryMovementUseCase;
import reve_back.application.ports.out.BottleRepositoryPort;
import reve_back.application.ports.out.ProductRepositoryPort;
import reve_back.domain.model.Bottle;
import reve_back.domain.model.InventoryMovement;
import reve_back.domain.model.MovementType;
import reve_back.domain.model.Product;
import reve_back.infrastructure.persistence.entity.InventoryMovementEntity;
import reve_back.infrastructure.persistence.jpa.SpringDataMovementRepository;
import reve_back.infrastructure.persistence.mapper.PersistenceMapper;
import reve_back.infrastructure.web.dto.QuickMovementRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService implements InventoryMovementUseCase {

    private final BottleRepositoryPort bottleRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final SpringDataMovementRepository movementRepository;
    private final PersistenceMapper mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processMovement(QuickMovementRequest request) {

        // 1. Validar y normalizar entradas
        String typeStr = request.type().toUpperCase().trim();
        String unit = request.unit().toUpperCase().trim();
        MovementType movementType;

        try {
            movementType = MovementType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error: Tipo de movimiento '" + typeStr + "' inválido. Use INGRESO o EGRESO.");
        }

        // 2. Obtener la botella actual y producto
        Bottle bottle = bottleRepositoryPort.findById(request.bottleId())
                .orElseThrow(() -> new RuntimeException("Error: La botella con ID " + request.bottleId() + " no existe."));

        Product product = productRepositoryPort.findById(bottle.productId())
                .orElseThrow(() -> new RuntimeException("Error: El producto no fue encontrado"));

        String currentStatus = bottle.status().toLowerCase();

        if (movementType == MovementType.INGRESO && "UNIT".equals(unit)) {
            if (currentStatus.contains("decant")) {
                throw new RuntimeException("Restricción: No se puede ingresar stock de UNIDADES a una fila destinada a DECANTS.");
            }
        }

        int newQty = bottle.quantity();
        int volumenProduct = product.volumeProductsMl();
        int newVolumeML = bottle.volumeMl();
        int newRemainingVolumeMl = bottle.remainingVolumeMl();

        if ("UNIT".equals(unit)) {
            // --- MOVIMIENTO DE UNIDADES FÍSICAS ---
            if (movementType == MovementType.INGRESO) {
                newQty = bottle.quantity() + request.quantity();
                newVolumeML += newQty*volumenProduct;
            } else {
                newQty = bottle.quantity() - request.quantity();
                newVolumeML -= newQty*volumenProduct;
                if (newQty < 0) {
                    throw new RuntimeException("Stock insuficiente: No tienes suficientes unidades físicas para esta salida.");
                }
            }
            newRemainingVolumeMl= newVolumeML;
        }
        else if ("ML".equals(unit)) {
            if (movementType == MovementType.INGRESO) {
                newRemainingVolumeMl = bottle.remainingVolumeMl() + request.quantity();
                if (newRemainingVolumeMl > bottle.volumeMl()) {
                    throw new RuntimeException("Error: El volumen resultante (" + newRemainingVolumeMl + "ml) excede la capacidad de la botella (" + bottle.volumeMl() + "ml).");
                }
            } else {
                newRemainingVolumeMl = bottle.remainingVolumeMl() - request.quantity();
                if (newRemainingVolumeMl < 0) {
                    throw new RuntimeException("Volumen insuficiente: La botella solo tiene " + bottle.remainingVolumeMl() + "ml disponibles.");
                }
            }
        }
        else {
            throw new RuntimeException("Error: Unidad '" + unit + "' no reconocida. Use 'UNIT' o 'ML'.");
        }

        String newStatus = bottle.status();

        if (newQty == 0 && "UNIT".equals(unit)) {
            newStatus = "agotada";

        }
        else if (newRemainingVolumeMl == 0 && "ML".equals(unit)) {
            newStatus = "decant-agotada";
            newVolumeML=0;
            newQty=0;
        }
        else if (movementType == MovementType.INGRESO) {
            if (currentStatus.equals("agotada") && newQty > 0) newStatus = "sellada";
            if (currentStatus.equals("decant-agotada") && newRemainingVolumeMl > 0){
                newStatus = "decantada";
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
        history.setUnit(unit);
        history.setReason(request.reason().toUpperCase().trim());
        history.setUserId(request.userId());

        movementRepository.save(history);
    }
}
