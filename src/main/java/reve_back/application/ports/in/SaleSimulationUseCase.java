package reve_back.application.ports.in;

import reve_back.domain.model.CartItem;
import reve_back.infrastructure.web.dto.SaleSimulationResponse;

import java.util.List;

public interface SaleSimulationUseCase {
    SaleSimulationResponse calculateSimulation(List<CartItem> cart, Long promotionId);
}
