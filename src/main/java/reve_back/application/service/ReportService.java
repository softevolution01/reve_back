package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reve_back.application.ports.in.GetDailyReportUseCase;
import reve_back.infrastructure.persistence.entity.BranchEntity;
import reve_back.infrastructure.persistence.entity.CashMovementEntity;
import reve_back.infrastructure.persistence.entity.SaleEntity;
import reve_back.infrastructure.persistence.entity.SalePaymentEntity;
import reve_back.infrastructure.persistence.jpa.BranchJpaRepository;
import reve_back.infrastructure.persistence.jpa.CashMovementJpaRepository;
import reve_back.infrastructure.persistence.jpa.SalePaymentJpaRepository;
import reve_back.infrastructure.persistence.jpa.SalesJpaRepository;
import reve_back.infrastructure.web.dto.DailyReportResponse;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ReportService implements GetDailyReportUseCase {

    private final CashMovementJpaRepository cashRepo;
    private final SalesJpaRepository salesRepo;
    private final SalePaymentJpaRepository paymentsRepo;
    private final BranchJpaRepository branchJpaRepository;

    @Override
    public DailyReportResponse getSummary() {
        // 0. Precargar nombres de sedes en un Mapa (ID -> Nombre) para mayor velocidad
        Map<Long, String> branchMap = branchJpaRepository.findAll().stream()
                .collect(Collectors.toMap(BranchEntity::getId, BranchEntity::getName));

        // 1. Saldo en Caja Maestra (Sede 1): Ingresos - Egresos
        BigDecimal ingresos = cashRepo.findAll().stream()
                .filter(m -> m.getBranchId() != null && m.getBranchId() == 1L && "INGRESO".equals(m.getType()))
                .map(CashMovementEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal egresos = cashRepo.findAll().stream()
                .filter(m -> m.getBranchId() != null && m.getBranchId() == 1L && "EGRESO".equals(m.getType()))
                .map(CashMovementEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldo = ingresos.subtract(egresos);

        // 2. Ventas Legales por Sede (Ahora con NOMBRES reales)
        Map<String, BigDecimal> ventasSede = salesRepo.findAll().stream()
                .collect(Collectors.groupingBy(
                        s -> {
                            if (s.getBranchId() == null) return "Ventas sin Sede";
                            // Buscamos el nombre en el mapa que precargamos arriba
                            return branchMap.getOrDefault(s.getBranchId(), "Sede Desconocida (ID: " + s.getBranchId() + ")");
                        },
                        Collectors.mapping(SaleEntity::getTotalAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        // 3. Ventas por Medio de Pago (Saber cuánto hay en Yape vs Efectivo)
        Map<String, BigDecimal> ventasMedio = paymentsRepo.findAll().stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPaymentMethod().getName(),
                        Collectors.mapping(SalePaymentEntity::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        return new DailyReportResponse(saldo, ventasSede, ventasMedio);
    }
}
