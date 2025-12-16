package reve_back.application.ports.out;

import reve_back.domain.model.Bottle;

import java.util.List;
import java.util.Optional;

public interface BottleRepositoryPort {
    List<Bottle> saveAll(List<Bottle> bottles);
    List<Bottle> findAllByProductId(Long productId);
    List<Bottle> updateAll(List<Bottle> bottles);
    Optional<Bottle> findByBarcodeAndStatus(String barcode, String status);
}
