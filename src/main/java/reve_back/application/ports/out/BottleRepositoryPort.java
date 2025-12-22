package reve_back.application.ports.out;

import jakarta.validation.constraints.NotNull;
import reve_back.domain.model.Bottle;
import reve_back.domain.model.BottlesStatus;

import java.util.List;
import java.util.Optional;

public interface BottleRepositoryPort {
    List<Bottle> saveAll(List<Bottle> bottles);
    List<Bottle> findAllByProductId(Long productId);
    List<Bottle> updateAll(List<Bottle> bottles);
    Optional<Bottle> findByBarcodeAndStatus(String barcode, BottlesStatus status);
    Optional<Bottle> findById(Long id);
    Bottle save(Bottle bottle);
    List<Bottle> searchActiveByProductName(String term);

}
