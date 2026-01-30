package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.CashMovementRepositoryPort;
import reve_back.domain.model.CashMovement;
import reve_back.infrastructure.mapper.CashMovementDtoMapper;
import reve_back.infrastructure.persistence.entity.CashMovementEntity;
import reve_back.infrastructure.persistence.enums.global.CashMovementType;
import reve_back.infrastructure.persistence.jpa.SpringDataCashMovementRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class JpaCashMovementRepositoryAdapter implements CashMovementRepositoryPort {

    private final SpringDataCashMovementRepository jpaRepository;
    private final CashMovementDtoMapper mapper;

    @Override
    public CashMovement save(CashMovement cashMovement) {
        // 1. Convertimos el Record de dominio a Entidad JPA
        CashMovementEntity entity = mapper.toEntity(cashMovement);

        // 2. Guardamos en la base de datos
        CashMovementEntity savedEntity = jpaRepository.save(entity);

        // 3. RETORNAMOS el dominio actualizado (con ID generado)
        return mapper.toDomain(savedEntity);
    }

    @Override
    public BigDecimal sumTotalBySessionAndType(Long sessionId, String type) {
        // Convertimos el String ("INGRESO") al Enum que usa JPA (CashMovementType.INGRESO)
        CashMovementType enumType = CashMovementType.valueOf(type);

        // Llamamos al repositorio de Spring Data
        BigDecimal total = jpaRepository.sumAmountBySessionAndType(sessionId, enumType);

        // Manejamos el null por si no hay registros
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public List<CashMovement> findRecentBySession(Long sessionId) {
        // Obtenemos las entidades y las mapeamos a dominio
        return jpaRepository.findTop10ByCashSessionIdOrderByCreatedAtDesc(sessionId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

}
