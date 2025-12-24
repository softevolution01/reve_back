package reve_back.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "clients")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullname;

    @Column(unique = true)
    private String dni;

    private String email;

    private String phone;

    @Column(name = "is_vip")
    @Builder.Default
    private Boolean isVip = false;

    @Column(name = "vip_since")
    private LocalDateTime vipSince;

    @Column(name = "vip_purchase_counter")
    @Builder.Default
    private Integer vipPurchaseCounter = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumn
    private ClientLoyaltyProgressEntity loyalty;
}
