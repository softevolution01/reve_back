package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.out.CashMovementRepositoryPort;
import reve_back.application.ports.out.PaymentMethodsRepositoryPort;
import reve_back.domain.model.Branch;
import reve_back.domain.model.CashMovement;
import reve_back.domain.model.PaymentMethod;
import reve_back.domain.model.SalePayment;
import reve_back.infrastructure.web.dto.PaymentRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaymentProcessingService {

    private final PaymentMethodsRepositoryPort paymentMethodRepositoryPort;
    private final CashMovementRepositoryPort cashMovementRepositoryPort;

    @Transactional
    public PaymentResult processPayments(List<PaymentRequest> payments, Branch branch, Long userId) {
        List<SalePayment> salePayments = new ArrayList<>();
        BigDecimal totalSurcharge = BigDecimal.ZERO;
        Set<String> methodNames = new HashSet<>();

        for (PaymentRequest pReq : payments) {
            PaymentMethod pm = paymentMethodRepositoryPort.findById(pReq.paymentMethodId())
                    .orElseThrow(() -> new RuntimeException("Método de pago no encontrado"));

            methodNames.add(pm.name().toUpperCase());

            BigDecimal surcharge = BigDecimal.ZERO;
            if (pm.surchargePercentage() != null && pm.surchargePercentage().compareTo(BigDecimal.ZERO) > 0) {
                surcharge = pReq.amount().multiply(pm.surchargePercentage())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                totalSurcharge = totalSurcharge.add(surcharge);
            }

            salePayments.add(new SalePayment(null, pReq.paymentMethodId(), pm.name(), pReq.amount(), surcharge));

            if (pm.name().equalsIgnoreCase("Efectivo")) {
                Long destBranchId = Boolean.TRUE.equals(branch.isCashManagedCentralized()) ? 1L : branch.id();
                CashMovement movement = new CashMovement(null, destBranchId, pReq.amount(),
                        "INGRESO", "Venta en Sede: " + branch.name(), userId, null, LocalDateTime.now());
                cashMovementRepositoryPort.save(movement);
            }
        }

        String finalMethodString = (methodNames.size() > 1) ? "MIXTO" : methodNames.iterator().next();
        return new PaymentResult(salePayments, totalSurcharge, finalMethodString);
    }

    public record PaymentResult(List<SalePayment> salePayments, BigDecimal totalSurcharge, String paymentMethodString) {}
}