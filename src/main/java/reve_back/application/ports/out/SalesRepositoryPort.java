package reve_back.application.ports.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface SalesRepositoryPort {
    BigDecimal getTotalSalesByClient(Long clientId);
    BigDecimal getTotalSalesByClientAfterDate(Long clientId, LocalDateTime date);
}
