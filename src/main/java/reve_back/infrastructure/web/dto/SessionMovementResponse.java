package reve_back.infrastructure.web.dto;

import reve_back.domain.model.CashMovementItem;

import java.math.BigDecimal;
import java.util.List;

public record SessionMovementResponse(
        Long id,
        String time,
        String type,
        String description,
        BigDecimal amount,
        String method,
        String registeredByName,
        Long saleId,
        Long contractId,
        List<CashMovementItem> items
) {}
