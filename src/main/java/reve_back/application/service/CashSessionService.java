package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.in.ManageCashSessionUseCase;
import reve_back.application.ports.out.BranchRepositoryPort;
import reve_back.application.ports.out.CashMovementRepositoryPort;
import reve_back.application.ports.out.CashSessionRepositoryPort;
import reve_back.application.ports.out.SalesRepositoryPort;
import reve_back.domain.model.CashMovement;
import reve_back.domain.model.CashSession;
import reve_back.domain.model.User;
import reve_back.infrastructure.persistence.entity.UserEntity;
import reve_back.infrastructure.persistence.enums.global.SessionStatus;
import reve_back.infrastructure.persistence.jpa.PaymentMethodSummary;
import reve_back.infrastructure.persistence.jpa.SpringDataSaleRepository;
import reve_back.infrastructure.web.dto.CashStatusResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CashSessionService implements ManageCashSessionUseCase {

    private final CashSessionRepositoryPort cashSessionPort;
    private final CashMovementRepositoryPort cashMovementPort;

    // Repositorios auxiliares (Idealmente deberían ser puertos también)
    private final BranchRepositoryPort branchPort;
    private final SalesRepositoryPort salesRepository;
    private final SpringDataSaleRepository springDataSaleRepository;

    @Override
    @Transactional(readOnly = true)
    public CashStatusResponse getSessionStatus(Long branchId) {
        // 1. Obtener Almacén
        var branch = branchPort.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));

        Long warehouseId = branch.warehouseId();

        // 2. Buscar sesión
        var sessionOpt = cashSessionPort.findOpenSessionByWarehouse(warehouseId);

        if (sessionOpt.isEmpty()) {
            // Caso CERRADO: Retornamos todo en cero y listas vacías
            return new CashStatusResponse(
                    "CLOSED",
                    null,
                    null, // openedAt
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    Collections.emptyMap(), // Mapa de desglose vacío
                    Collections.emptyList()
            );
        }

        CashSession session = sessionOpt.get();
        Long sessionId = session.getId();

        // 3. CÁLCULOS AVANZADOS (Desglose por método de pago)

        // A. Obtenemos el desglose agrupado desde Ventas
        // Esto separa cuanto fue Efectivo, Yape, Visa, etc.
        List<PaymentMethodSummary> breakdownList = springDataSaleRepository.getPaymentBreakdownBySession(sessionId);

        // B. Convertimos a Mapa para enviarlo al Frontend y calcular fácil
        Map<String, BigDecimal> breakdownMap = breakdownList.stream()
                .collect(Collectors.toMap(
                        summary -> summary.getMethod().toUpperCase(), // Clave: "EFECTIVO", "YAPE"
                        PaymentMethodSummary::getTotal                // Valor: Monto
                ));

        // C. Extraemos SOLO lo que es EFECTIVO para el arqueo físico
        // Importante: Asegúrate que en tu BD el método se llame "EFECTIVO"
        BigDecimal totalSalesCash = breakdownMap.getOrDefault("EFECTIVO", BigDecimal.ZERO);

        // D. Movimientos Manuales (Ingresos/Egresos de caja chica)
        BigDecimal totalIncome = cashMovementPort.sumTotalBySessionAndType(sessionId, "INGRESO");
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;

        BigDecimal totalExpense = cashMovementPort.sumTotalBySessionAndType(sessionId, "EGRESO");
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;

        // 4. CÁLCULO DEL SALDO FÍSICO (Billetes en el cajón)
        // Fórmula: Inicial + Ventas(Solo Efectivo) + Ingresos Manuales - Egresos Manuales
        BigDecimal currentBalance = session.getInitialCash()
                .add(totalSalesCash)
                .add(totalIncome)
                .subtract(totalExpense);

        // 5. Historial de movimientos manuales recientes
        List<CashMovement> recentMovements = cashMovementPort.findRecentBySession(sessionId);

        return new CashStatusResponse(
                "OPEN",
                sessionId,
                session.getInitialCash(),
                totalSalesCash,
                totalIncome,
                totalExpense,
                currentBalance,
                breakdownMap,
                recentMovements
        );
    }

    @Override
    public void openSession(Long branchId, Long userId, BigDecimal initialAmount) {
        var branch = branchPort.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));

        Long warehouseId = branch.warehouseId();

        // VALIDACIÓN DE NEGOCIO: Solo una caja por almacén
        if (cashSessionPort.existsOpenSessionByWarehouse(warehouseId)) {
            throw new RuntimeException("La caja de este almacén ya se encuentra abierta.");
        }

        // Crear Objeto de Dominio
        CashSession newSession = new CashSession();
        newSession.setWarehouseId(warehouseId);
        newSession.setOpenedByUserId(userId);
        newSession.setInitialCash(initialAmount);
        newSession.setOpenedAt(LocalDateTime.now());
        newSession.setStatus(SessionStatus.OPEN);

        cashSessionPort.save(newSession);
    }

    @Override
    public void closeSession(Long branchId, Long userId, BigDecimal countedCash, String notes) {

        CashStatusResponse status = getSessionStatus(branchId);

        if (!"OPEN".equals(status.status())) {
            throw new RuntimeException("No hay una sesión abierta para cerrar.");
        }

        var branch = branchPort.findById(branchId).orElseThrow();
        var session = cashSessionPort.findOpenSessionByWarehouse(branch.warehouseId()).get();

        BigDecimal expected = status.currentSystemBalance();
        BigDecimal difference = countedCash.subtract(expected);

        session.setClosedAt(LocalDateTime.now());

        session.setClosedByUserId(userId);

        session.setFinalCashExpected(expected);
        session.setFinalCashCounted(countedCash);
        session.setDifference(difference);
        session.setNotes(notes);
        session.setStatus(SessionStatus.CLOSED);

        cashSessionPort.save(session);
    }

    @Override
    public void registerMovement(Long branchId, Long userId, String type, BigDecimal amount, String description, String method) {
        var branch = branchPort.findById(branchId).orElseThrow();
        Long warehouseId = branch.warehouseId();

        // Verificar que la caja esté abierta
        var session = cashSessionPort.findOpenSessionByWarehouse(warehouseId)
                .orElseThrow(() -> new RuntimeException("Debe abrir la caja antes de registrar movimientos."));

        CashMovement movement = new CashMovement(
                null,
                session.getId(),
                branchId,
                amount,
                type,
                description,
                method,
                userId,
                null,
                null, // saleId
                LocalDateTime.now()
        );

        cashMovementPort.save(movement);
    }
}
