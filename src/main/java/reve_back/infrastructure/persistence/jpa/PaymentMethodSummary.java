package reve_back.infrastructure.persistence.jpa;

import java.math.BigDecimal;

public interface PaymentMethodSummary {

    String getMethod();

    BigDecimal getTotal();
}
