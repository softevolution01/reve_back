package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reve_back.infrastructure.persistence.entity.BranchEntity;

import java.util.Set;

@RepositoryRestResource(exported = false)
public interface BranchJpaRepository extends JpaRepository<BranchEntity, Long> {
    Set<BranchEntity> findAllByNameIn(Set<String> names);
}
