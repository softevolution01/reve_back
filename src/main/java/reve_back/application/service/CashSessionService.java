package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.in.ManageCashSessionUseCase;
import reve_back.application.ports.out.*;
import reve_back.domain.model.CashMovement;
import reve_back.domain.model.CashSession;
import reve_back.infrastructure.mapper.CashSessionMapper;
import reve_back.infrastructure.persistence.entity.CashSessionEntity;
import reve_back.infrastructure.persistence.entity.CashSessionsSummaryEntity;
import reve_back.infrastructure.persistence.entity.PaymentMethodEntity;
import reve_back.infrastructure.persistence.enums.global.SessionStatus;
import reve_back.infrastructure.persistence.jpa.*;
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
    private final SprigDataPaymentMethodRepository sprigDataPaymentMethodRepository;
    private final SpringDataCashSessionSummaryRepository springDataCashSessionSummaryRepository;
    private final CashSessionMapper cashSessionMapper;
    private final SpringDataCashMovementRepository springDataCashMovementRepository;

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
            return new CashStatusResponse(
                    "CLOSED",
                    null,
                    BigDecimal.ZERO, // initialCash (Corregido orden según tu Record anterior)
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    Collections.emptyMap(),
                    Collections.emptyList()
            );
        }

        CashSession session = sessionOpt.get();
        Long sessionId = session.getId();

        List<PaymentMethodSummary> breakdownList = springDataCashMovementRepository.getVentaBreakdownBySession(sessionId);

        // B. Convertimos a Mapa para enviarlo al Frontend
        Map<String, BigDecimal> breakdownMap = breakdownList.stream()
                .collect(Collectors.toMap(
                        summary -> summary.getMethod() != null ? summary.getMethod().toUpperCase() : "DESCONOCIDO",
                        PaymentMethodSummary::getTotal
                ));

        BigDecimal totalOperationsCash = breakdownMap.getOrDefault("EFECTIVO", BigDecimal.ZERO);

        // D. Movimientos Manuales (Ingresos/Egresos de caja chica)
        BigDecimal totalManualIncome = springDataCashMovementRepository.sumTotalIncomeBySession(sessionId);
        BigDecimal totalManualExpense = springDataCashMovementRepository.sumTotalExpenseBySession(sessionId);

        // E. Calculamos el Saldo Físico Real
        // Fórmula: Inicial + (Ventas/Contratos en Efectivo) + (Ingresos Manuales) - (Gastos Manuales)
        BigDecimal currentBalance = session.getInitialCash()
                .add(totalOperationsCash)
                .add(totalManualIncome)
                .subtract(totalManualExpense);

        // 5. Historial de movimientos recientes
        List<CashMovement> recentMovements = cashMovementPort.findRecentBySession(sessionId);

        return new CashStatusResponse(
                "OPEN",
                sessionId,
                session.getInitialCash(),
                totalOperationsCash,
                totalManualIncome,
                totalManualExpense,
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
    @Transactional
    public void closeSession(Long branchId, Long userId, BigDecimal countedCash, String notes) {

        // 1. Obtenemos estado actual (con el desglose calculado)
        CashStatusResponse status = getSessionStatus(branchId);

        if (!"OPEN".equals(status.status())) {
            throw new RuntimeException("No hay una sesión abierta para cerrar.");
        }

        var branch = branchPort.findById(branchId).orElseThrow();
        var session = cashSessionPort.findOpenSessionByWarehouse(branch.warehouseId()).get();

        // 2. Cálculos de Diferencia (Solo afecta al Efectivo)
        BigDecimal expectedPhysical = status.currentSystemBalance();
        BigDecimal difference = countedCash.subtract(expectedPhysical);

        // 3. Actualizamos la sesión principal
        session.setClosedAt(LocalDateTime.now());
        session.setClosedByUserId(userId);
        session.setFinalCashExpected(expectedPhysical);
        session.setFinalCashCounted(countedCash);
        session.setDifference(difference);
        session.setTotalManualIncome(status.totalIncome());
        session.setTotalManualExpense(status.totalExpense());
        session.setNotes(notes);
        session.setStatus(SessionStatus.CLOSED);

        CashSession savedDomainSession = cashSessionPort.save(session);

        CashSessionEntity savedSession = cashSessionMapper.toEntity(savedDomainSession);

        if (status.paymentBreakdown() != null && !status.paymentBreakdown().isEmpty()) {

            List<PaymentMethodEntity> allMethods = sprigDataPaymentMethodRepository.findAll();

            status.paymentBreakdown().forEach((methodName, amount) -> {

                PaymentMethodEntity pmEntity = allMethods.stream()
                        .filter(pm -> pm.getName().equalsIgnoreCase(methodName))
                        .findFirst()
                        .orElse(null);

                if (pmEntity != null && amount.compareTo(BigDecimal.ZERO) > 0) {

                    CashSessionsSummaryEntity summary = CashSessionsSummaryEntity.builder()
                            .cashSession(savedSession)
                            .paymentMethod(pmEntity)
                            .amount(amount)
                            .build();

                    springDataCashSessionSummaryRepository.save(summary);
                }
            });
        }
    }

    @Override
    public void registerMovement(Long branchId, Long userId, String type, BigDecimal amount, String description, String method) {
        var branch = branchPort.findById(branchId).orElseThrow();
        Long warehouseId = branch.warehouseId();

        var session = cashSessionPort.findOpenSessionByWarehouse(warehouseId)
                .orElseThrow(() -> new RuntimeException("Debe abrir la caja antes de registrar movimientos."));

        String finalMethod = method;
        if (finalMethod == null || finalMethod.trim().isEmpty()) {
            finalMethod = "EFECTIVO";
        }

        CashMovement movement = new CashMovement(
                null,
                session.getId(),
                branchId,
                amount,
                type,
                description,
                finalMethod,
                userId,
                null,
                null, // saleId
                LocalDateTime.now()
        );

        cashMovementPort.save(movement);
    }
}
