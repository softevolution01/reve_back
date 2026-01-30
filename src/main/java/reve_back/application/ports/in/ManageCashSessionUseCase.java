package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.CashStatusResponse;

import java.math.BigDecimal;

public interface ManageCashSessionUseCase {

    CashStatusResponse getSessionStatus(Long branchId);
    void openSession(Long branchId, Long userId, BigDecimal initialAmount);
    void closeSession(Long branchId, Long userId, BigDecimal countedCash, String notes);
    void registerMovement(Long branchId, Long userId, String type, BigDecimal amount, String description, String method);
}
