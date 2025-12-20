package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.SaleSimulationRequest;

public interface CreateSaleUseCase {
    /**
     * Ejecuta la creación de una venta:
     * 1. Valida precios.
     * 2. Guarda la venta.
     * 3. Descuenta inventario.
     * 4. Registra caja.
     * @return El ID de la venta creada.
     */
    Long execute(SaleSimulationRequest request);
}
