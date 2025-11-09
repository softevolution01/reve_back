package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import reve_back.infrastructure.persistence.entity.BranchEntity;

import java.util.Set;

public interface BranchJpaRepository extends JpaRepository<BranchEntity, Long> {
    Set<BranchEntity> findAllByNameIn(Set<String> names);
}
