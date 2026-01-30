package reve_back.application.ports.out;

import reve_back.domain.model.Sale;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface SalesRepositoryPort {
    BigDecimal getTotalSalesByClient(Long clientId);
    BigDecimal getTotalSalesByClientAfterDate(Long clientId, LocalDateTime date);

    Sale save(Sale sale);
    Optional<Sale> findById(Long id);
    BigDecimal sumCashSalesBySessionId(Long sessionId);
}
