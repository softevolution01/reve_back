package reve_back.domain.model;

import lombok.*;
import reve_back.infrastructure.persistence.enums.global.SessionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // Patrón Builder para facilitar la creación en el Mapper
public class CashSession {

    private Long id;

    // El núcleo de tu lógica: Almacén Físico
    private Long warehouseId;

    // Datos de Apertura
    private Long openedByUserId;
    private LocalDateTime openedAt;
    private BigDecimal initialCash;

    // Datos de Cierre
    private LocalDateTime closedAt;
    private Long closedByUserId;

    // Arqueo de Caja
    private BigDecimal finalCashExpected; // Lo que el sistema dice que hay
    private BigDecimal finalCashCounted;  // Lo que el usuario contó
    private BigDecimal difference;        // Sobrante o Faltante
    private BigDecimal totalManualIncome;
    private BigDecimal totalManualExpense;

    private String notes;

    // Estado
    private SessionStatus status;
}
