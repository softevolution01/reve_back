package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reve_back.infrastructure.persistence.entity.RoleEntity;

import java.util.Optional;

@RepositoryRestResource(exported = false)
public interface RoleJpaRepository extends JpaRepository<RoleEntity, Integer> {
    Optional<RoleEntity> findByName(String name);
}
