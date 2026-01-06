package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.InventoryAlertResponse;

import java.util.List;

public interface GetInventoryAlertsUseCase {
    List<InventoryAlertResponse> getInventoryAlerts();
}