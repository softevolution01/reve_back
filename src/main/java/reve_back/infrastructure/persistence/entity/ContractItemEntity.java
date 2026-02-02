package reve_back.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "contract_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "contract_id")
    private ContractEntity contract;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}