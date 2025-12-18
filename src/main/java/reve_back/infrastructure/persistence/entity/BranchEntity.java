package reve_back.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "branches")
public class BranchEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(nullable = false)
    private String name;

    private String location;

    @Column(name = "is_cash_managed_centralized", nullable = false)
    private Boolean isCashManagedCentralized = false;

    @ManyToMany(mappedBy = "branches", fetch = FetchType.LAZY)
    private Set<UserEntity> users = new HashSet<>();
}
