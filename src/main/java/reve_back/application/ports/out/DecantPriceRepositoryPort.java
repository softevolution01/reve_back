package reve_back.application.ports.out;

import reve_back.domain.model.DecantPrice;
import reve_back.infrastructure.persistence.entity.DecantPriceEntity;
import reve_back.infrastructure.web.dto.DecantRequest;

import java.util.List;
import java.util.Optional;

public interface DecantPriceRepositoryPort {
    List<DecantPrice> saveAllForProduct(Long productId, List<DecantPrice> decants);

    List<DecantPrice> findAllByProductId(Long productId);

    Optional<DecantPrice> findByBarcode(String barcode);

}
