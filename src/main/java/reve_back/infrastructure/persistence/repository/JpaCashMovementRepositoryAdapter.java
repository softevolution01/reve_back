package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.CashMovementRepositoryPort;
import reve_back.domain.model.CashMovement;
import reve_back.infrastructure.mapper.CashMovementDtoMapper;
import reve_back.infrastructure.persistence.entity.CashMovementEntity;
import reve_back.infrastructure.persistence.jpa.SpringDataCashMovementRepository;

@RequiredArgsConstructor
@Repository
public class JpaCashMovementRepositoryAdapter implements CashMovementRepositoryPort {

    private final SpringDataCashMovementRepository jpaRepository;
    private final CashMovementDtoMapper mapper;

    @Override
    public void save(CashMovement cashMovement) {
        // 1. Convertimos el Record de dominio a Entidad JPA
        CashMovementEntity entity = mapper.toEntity(cashMovement);

        // 2. Guardamos en la base de datos
        jpaRepository.save(entity);
    }

}
