package reve_back.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "client_loyalty_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientLoyaltyProgressEntity {

    @Id
    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "current_tier", nullable = false)
    private Integer currentTier = 1;

    @Column(name = "points_in_tier", nullable = false)
    private Integer pointsInTier = 0;

    @Column(name = "accumulated_money", nullable = false)
    private Double accumulatedMoney;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
