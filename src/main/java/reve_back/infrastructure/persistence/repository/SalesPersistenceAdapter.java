package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.SalesRepositoryPort;
import reve_back.infrastructure.persistence.entity.SaleEntity;
import reve_back.infrastructure.persistence.jpa.SalesJpaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Repository
public class SalesPersistenceAdapter implements SalesRepositoryPort {

    private final SalesJpaRepository salesJpaRepository;

    @Override
    public SaleEntity saveSale(SaleEntity sale) {
        return salesJpaRepository.save(sale);
    }

    @Override
    public BigDecimal getTotalSalesByClient(Long clientId) {
        return salesJpaRepository.sumTotalAmountByClientId(clientId);
    }

    @Override
    public BigDecimal getTotalSalesByClientAfterDate(Long clientId, LocalDateTime date) {
        return salesJpaRepository.sumTotalAmountByClientIdAndDateAfter(clientId, date);
    }
}
