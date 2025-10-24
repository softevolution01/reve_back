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
@Table(name = "products")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brand;

    private String line;

    private String concentration;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean is_active;

    @Column(name = "created_at")
    @CreationTimestamp
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private java.time.LocalDateTime updatedAt;

}
