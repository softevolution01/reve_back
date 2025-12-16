package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.WarehouseRepositoryPort;
import reve_back.domain.model.Product;
import reve_back.domain.model.Warehouse;
import reve_back.infrastructure.persistence.jpa.SpringDataWarehouseRepository;
import reve_back.infrastructure.persistence.mapper.PersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class JpaWarehouseRepositoryAdapter implements WarehouseRepositoryPort {

    private final SpringDataWarehouseRepository springDataWarehouseRepository;
    private final PersistenceMapper persistenceMapper;


    @Override
    public List<Warehouse> findAll() {
        return springDataWarehouseRepository.findAll().stream()
                .map(persistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Warehouse> findById(Long id) {
        return springDataWarehouseRepository.findById(id)
                .map(persistenceMapper::toDomain);
    }
}
