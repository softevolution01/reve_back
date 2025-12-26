package reve_back.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "loyalty_tiers")
@Builder
public class LoyaltyTiersEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tier_level", unique = true, nullable = false)
    private Integer tierLevel;

    @Column(name = "cost_per_point", nullable = false)
    private BigDecimal costPerPoint;
}
