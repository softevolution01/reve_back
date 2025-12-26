package reve_back.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "decant_prices")
public class DecantPriceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "volume_ml", nullable = false)
    private Integer volumeMl;

    @Column(nullable = false)
    private Double price;

    private String barcode;
    @Column(name = "image_barcode")
    private String imageBarcode;
}
