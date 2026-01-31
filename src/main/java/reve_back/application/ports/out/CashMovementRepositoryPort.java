package reve_back.application.ports.out;

import reve_back.domain.model.CashMovement;

import java.math.BigDecimal;
import java.util.List;

public interface CashMovementRepositoryPort {
    CashMovement save(CashMovement movement);

    // FALTABAN ESTOS MÉTODOS:
    BigDecimal sumTotalBySessionAndType(Long sessionId, String type);

    // Ojo: Recibe ID (Long), no el objeto Session completo
    List<CashMovement> findRecentBySession(Long sessionId);

    List<CashMovement> findAllBySessionId(Long sessionId);
}