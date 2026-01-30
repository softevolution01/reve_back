package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.SalesRepositoryPort;
import reve_back.domain.model.Sale;
import reve_back.infrastructure.mapper.SaleDtoMapper;
import reve_back.infrastructure.persistence.entity.SaleEntity;
import reve_back.infrastructure.persistence.jpa.SalesJpaRepository;
import reve_back.infrastructure.persistence.jpa.SpringDataSaleRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class SalesPersistenceAdapter implements SalesRepositoryPort {

    private final SalesJpaRepository salesJpaRepository;
    private final SpringDataSaleRepository springDataSaleRepository;
    private final SaleDtoMapper saleDtoMapper;

    @Override
    public BigDecimal getTotalSalesByClient(Long clientId) {
        return salesJpaRepository.sumTotalAmountByClientId(clientId);
    }

    @Override
    public BigDecimal getTotalSalesByClientAfterDate(Long clientId, LocalDateTime date) {
        return salesJpaRepository.sumTotalAmountByClientIdAndDateAfter(clientId, date);
    }


    @Override
    public Sale save(Sale saleDomain) {
        SaleEntity entityToSave = saleDtoMapper.toEntity(saleDomain);

        SaleEntity savedEntity = springDataSaleRepository.save(entityToSave);

        return saleDtoMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Sale> findById(Long id) {
        return springDataSaleRepository.findById(id)
                .map(saleDtoMapper::toDomain);
    }

    @Override
    public BigDecimal sumCashSalesBySessionId(Long sessionId) {
        // Llamada directa a la query JPQL que creamos arriba
        return springDataSaleRepository.sumTotalByCashSessionId(sessionId);
    }
}
