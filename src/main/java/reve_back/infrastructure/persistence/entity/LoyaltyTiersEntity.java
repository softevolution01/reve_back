package reve_back.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "loyalty_tiers")
@Builder
public class LoyaltyTiersEntity {

    @Id
    @Column(nullable = false, name = "tier_level")
    private Integer tierLevel;

    @Column(nullable = false, name = "cost_per_point")
    private Double costPerPoint;
}
