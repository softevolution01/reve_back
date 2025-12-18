package reve_back.application.ports.out;

import reve_back.infrastructure.persistence.entity.SaleEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface SalesRepositoryPort {
    SaleEntity saveSale(SaleEntity sale);
    BigDecimal getTotalSalesByClient(Long clientId);
    BigDecimal getTotalSalesByClientAfterDate(Long clientId, LocalDateTime date);
}
