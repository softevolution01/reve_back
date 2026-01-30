package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.CashSessionRepositoryPort;
import reve_back.domain.model.CashSession;
import reve_back.infrastructure.mapper.CashSessionMapper;
import reve_back.infrastructure.persistence.jpa.SpringDataCashSessionRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CashSessionPersistenceAdapter implements CashSessionRepositoryPort {

    private final SpringDataCashSessionRepository jpaRepository;
    private final CashSessionMapper mapper;

    @Override
    public CashSession save(CashSession cashSession) {
        var entity = mapper.toEntity(cashSession);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<CashSession> findOpenSessionByWarehouse(Long warehouseId) {
        // Asegúrate de que jpaRepository tenga este método o uno similar
        return jpaRepository.findOpenByWarehouse(warehouseId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsOpenSessionByWarehouse(Long warehouseId) {
        return jpaRepository.existsOpenByWarehouse(warehouseId);
    }

    @Override
    public Optional<CashSession> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }
}
