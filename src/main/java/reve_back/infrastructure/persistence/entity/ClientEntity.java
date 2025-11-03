package reve_back.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "clients")
public class ClientEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullname;

    @Column(unique = true)
    private String dni;

    @Column(unique = true)
    private String email;

    private String phone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_vip", nullable = false)
    private boolean isVip = false;

    @Column(name = "vip_since")
    private LocalDateTime vipSince;

    @Column(name = "vip_purchase_counter", nullable = false)
    private int vipPurchaseCounter = 0;

    @OneToOne(mappedBy = "client", fetch = FetchType.LAZY)
    private UserEntity user;
}
