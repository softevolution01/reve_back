package reve_back.application.ports.out;

import reve_back.domain.model.SalePayment;
import reve_back.domain.model.CashMovement;
import java.util.List;

public interface PaymentRepositoryPort {
    void saveSalePayments(List<SalePayment> payments);
    void saveCashMovement(CashMovement movement);
}
