package reve_back.application.ports.out;

import reve_back.domain.model.Product;
import reve_back.domain.model.Warehouse;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepositoryPort {
    List<Warehouse> findAll();
    Optional<Warehouse> findById(Long id);
}
