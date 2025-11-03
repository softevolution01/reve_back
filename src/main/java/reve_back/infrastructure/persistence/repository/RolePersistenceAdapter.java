package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.RoleRepositoryPort;
import reve_back.domain.model.Role;
import reve_back.infrastructure.persistence.jpa.RoleJpaRepository;
import reve_back.infrastructure.persistence.mapper.PersistenceMapper;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class RolePersistenceAdapter implements RoleRepositoryPort {

    private final RoleJpaRepository roleJpaRepository;
    private final PersistenceMapper mapper;

    @Override
    public Optional<Role> findByName(String name) {
        return roleJpaRepository.findByName(name)
                .map(mapper::toDomain);
    }
}
