package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reve_back.infrastructure.persistence.entity.BranchEntity;

@RepositoryRestResource(exported = false)
public interface SpringDataBranchRepository extends JpaRepository<BranchEntity, Long> {
}
