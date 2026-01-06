package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.DashboardRepositoryPort;
import reve_back.infrastructure.persistence.jpa.SalesJpaRepository;
import reve_back.infrastructure.persistence.jpa.SpringDataBottleRepository;
import reve_back.infrastructure.web.dto.BranchSalesReportResponse;
import reve_back.infrastructure.web.dto.InventoryAlertResponse;
import reve_back.infrastructure.web.dto.WorkerPerformanceResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Repository
public class JpaDashboardRepositoryAdapter implements DashboardRepositoryPort {

    private final SalesJpaRepository springDataSaleRepository;
    private final SpringDataBottleRepository springDataBottleRepository;

    @Override
    public List<BranchSalesReportResponse> getSalesByBranch(String periodFormat) {
        List<Map<String, Object>> results = springDataSaleRepository.getSalesByBranchRaw(periodFormat);

        return results.stream().map(row -> {
            String sucursal = (String) row.get("sucursal");
            String periodo = (String) row.get("periodo");

            BigDecimal totalSellados = (BigDecimal) row.get("totalSellados");
            BigDecimal totalDecants = (BigDecimal) row.get("totalDecants");
            BigDecimal totalGeneral = (BigDecimal) row.get("totalGeneral");

            String status = totalGeneral.compareTo(new BigDecimal("8000")) > 0 ? "ALERTA" : "NORMAL";

            return new BranchSalesReportResponse(
                    sucursal, periodo, totalSellados, totalDecants, totalGeneral, status
            );
        }).toList();
    }

    @Override
    public List<WorkerPerformanceResponse> getSalesByWorker(String periodFormat) {
        List<Map<String, Object>> results = springDataSaleRepository.getSalesByWorkerRaw(periodFormat);

        return results.stream().map(row -> {
            String trabajador = (String) row.get("trabajador");
            String periodo = (String) row.get("periodo");
            Long tickets = ((Number) row.get("cantidadTickets")).longValue();
            BigDecimal totalVendido = (BigDecimal) row.get("totalVendido");

            return new WorkerPerformanceResponse(trabajador, periodo, tickets, totalVendido);
        }).toList();
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
}
