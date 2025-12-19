package reve_back.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

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

    @OneToOne
    @MapsId
    @JoinColumn(name = "client_id")
    private ClientEntity client;

    @Column(name = "current_tier")
    @Builder.Default
    private Integer currentTier = 1;

    @Column(name = "points_in_tier")
    @Builder.Default
    private Integer pointsInTier = 0;

    @Column(name = "accumulated_money", precision = 10, scale = 2)
    private BigDecimal accumulatedMoney;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
