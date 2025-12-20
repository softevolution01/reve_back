package reve_back.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "payment_methods") @Data
public class PaymentMethodEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // "EFECTIVO", "TARJETA", "YAPE"

    @Column(name = "surcharge_percentage")
    private BigDecimal surchargePercentage; // Ej: 5.00 para Tarjeta, 0.00 para Efectivo
}