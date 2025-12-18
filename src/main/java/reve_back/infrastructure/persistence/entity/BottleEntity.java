package reve_back.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Setter
@Table(name = "bottles")
public class BottleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    private String status;

    private String barcode;

    @Column(name = "volume_ml", nullable = false)
    private Integer volumeMl;

    @Column(name = "remaining_volume_ml", nullable = false)
    private Integer remainingVolumeMl;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "created_at")
    @CreationTimestamp
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private java.time.LocalDateTime updatedAt;
}
