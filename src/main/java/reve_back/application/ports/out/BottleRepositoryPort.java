package reve_back.application.ports.out;

import reve_back.domain.model.Bottle;

import java.util.List;

public interface BottleRepositoryPort {
    List<Bottle> saveAll(List<Bottle> bottles);
}
