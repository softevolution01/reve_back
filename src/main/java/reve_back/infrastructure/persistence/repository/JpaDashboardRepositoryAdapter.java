package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.DashboardRepositoryPort;
import reve_back.domain.model.CashMovementItem;
import reve_back.infrastructure.persistence.entity.CashSessionEntity;
import reve_back.infrastructure.persistence.jpa.SalesJpaRepository;
import reve_back.infrastructure.persistence.jpa.SpringDataBottleRepository;
import reve_back.infrastructure.persistence.jpa.SpringDataCashSessionRepository;
import reve_back.infrastructure.persistence.jpa.WorkerRankingProjection;
import reve_back.infrastructure.persistence.entity.CashMovementEntity;
import reve_back.infrastructure.web.dto.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RequiredArgsConstructor
@Repository
public class JpaDashboardRepositoryAdapter implements DashboardRepositoryPort {

    private final SalesJpaRepository springDataSaleRepository;
    private final SpringDataBottleRepository springDataBottleRepository;
    private final SpringDataCashSessionRepository springDataCashSessionRepository;

    private static final Double DEFAULT_ALERT_THRESHOLD = 40.0;

    @Override
    public List<SessionReportResponse> getSessionHistory(LocalDateTime start, LocalDateTime end) {
        List<CashSessionEntity> entities = springDataCashSessionRepository.findSessionsByDateRange(start, end);

        return entities.stream()
                .map(this::mapToSessionResponse)
                .toList();
    }


    @Override
    public List<WorkerRankingResponse> getWorkerRanking(LocalDateTime start, LocalDateTime end) {
        // 1. Obtenemos los datos ligeros de la BD (Proyecciones)
        List<WorkerRankingProjection> rawData = springDataSaleRepository.getWorkerRankingByDateRange(start, end);

        // 2. Convertimos manualmente al DTO final (Mapeo seguro)
        return rawData.stream()
                .map(projection -> new WorkerRankingResponse(
                        projection.getWorkerName(),
                        projection.getTicketsCount(),
                        projection.getTotalSold()
                ))
                .toList();
    }

    @Override
    public List<InventoryAlertResponse> getInventoryAlerts(Double thresholdPercentage) {
        List<Map<String, Object>> results = springDataBottleRepository.getInventoryAlertsRaw(thresholdPercentage);

        return results.stream().map(row -> {
            String almacen = (String) row.get("almacen");
            String producto = (String) row.get("producto");
            String barcode = (String) row.get("barcode");
            Integer capacidad = ((Number) row.get("capacidad")).intValue();
            Integer restante = ((Number) row.get("restante")).intValue();
            Double porcentaje = ((Number) row.get("porcentaje")).doubleValue();

            return new InventoryAlertResponse(almacen, producto, barcode, capacidad, restante, porcentaje);
        }).toList();
    }


    private SessionReportResponse mapToSessionResponse(CashSessionEntity entity) {

        // 1. Mapeo de Summaries (Igual que antes)
        List<SessionSummaryResponse> summaries = Optional.ofNullable(entity.getSummaries())
                .orElse(Collections.emptyList())
                .stream()
                .map(s -> new SessionSummaryResponse(
                        s.getPaymentMethod().getName(),
                        s.getAmount()
                ))
                .toList();

        // 2. Mapeo de Movements (CON LOS ITEMS)
        List<SessionMovementResponse> movements = Optional.ofNullable(entity.getMovements())
                .orElse(Collections.emptyList())
                .stream()
                .sorted(Comparator.comparing(CashMovementEntity::getCreatedAt))
                .map(m -> {
                    // --- LÓGICA PARA OBTENER LOS ITEMS ---
                    List<CashMovementItem> items = new ArrayList<>();

                    // CASO A: Es una VENTA
                    if (m.getSale() != null && m.getSale().getItems() != null) {
                        items = m.getSale().getItems().stream()
                                .map(item -> new CashMovementItem(
                                        item.getProduct().getBrand(),
                                        item.getQuantity(),
                                        item.getUnitPrice(),
                                        item.getFinalSubtotal() // Usa el subtotal final de la venta
                                ))
                                .toList();
                    }
                    // CASO B: Es un CONTRATO (Adelanto o Finalización)
                    else if (m.getContract() != null && m.getContract().getItems() != null) {
                        items = m.getContract().getItems().stream()
                                .map(item -> new CashMovementItem(
                                        item.getProduct().getBrand(),
                                        item.getQuantity(),
                                        item.getUnitPrice(),
                                        item.getSubtotal() // Usa el subtotal del item del contrato
                                ))
                                .toList();
                    }

                    // --- CONSTRUCCIÓN DEL DTO ---
                    return new SessionMovementResponse(
                            m.getId(),
                            m.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")),
                            m.getType().name(),
                            m.getDescription(),
                            m.getAmount(),
                            m.getMethod() != null ? m.getMethod() : "EFECTIVO",
                            m.getRegisteredBy() != null ? m.getRegisteredBy().getUsername() : "Sistema", // Nombre usuario
                            m.getSale() != null ? m.getSale().getId() : null,         // ID Venta
                            m.getContract() != null ? m.getContract().getId() : null, // ID Contrato
                            items
                    );
                })
                .toList();

        // 3. Construcción del DTO Final
        return new SessionReportResponse(
                entity.getId(),
                entity.getWarehouse().getName(),
                entity.getOpenedAt(),
                entity.getClosedAt(),
                entity.getOpenedByUser().getFullname(),
                entity.getClosedByUser() != null ? entity.getClosedByUser().getFullname() : null,
                entity.getStatus().name(),
                entity.getInitialCash(),
                entity.getFinalCashCounted() != null ? entity.getFinalCashCounted() : BigDecimal.ZERO,
                entity.getTotalManualIncome() != null ? entity.getTotalManualIncome() : BigDecimal.ZERO,
                entity.getTotalManualExpense() != null ? entity.getTotalManualExpense() : BigDecimal.ZERO,
                entity.getDifference() != null ? entity.getDifference() : BigDecimal.ZERO,
                entity.getNotes(),
                summaries,
                movements
        );
    }
}
