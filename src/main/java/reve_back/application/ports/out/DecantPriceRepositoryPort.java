package reve_back.application.ports.out;

import reve_back.domain.model.DecantPrice;
import reve_back.infrastructure.persistence.entity.DecantPriceEntity;
import reve_back.infrastructure.web.dto.DecantRequest;

import java.util.List;

public interface DecantPriceRepositoryPort {
    List<DecantPrice> saveAllForProduct(Long productId, List<DecantRequest> decants);
    List<DecantPriceEntity> findAllByProductId(Long productId);

}
