package reve_back.domain.model;

import java.math.BigDecimal;

public record SalePayment(
        Long id,
        Long paymentMethodId, // ID de la tabla payment_methods (1=Efectivo, 2=Tarjeta)
        String methodName,    // "TARJETA", "EFECTIVO" (Para visualización)
        BigDecimal amount,    // Cuánto pagó con este método
        BigDecimal commission // Comisión aplicada (ej: el 5% de este monto)
) {}
