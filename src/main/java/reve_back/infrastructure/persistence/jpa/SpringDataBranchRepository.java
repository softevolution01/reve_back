package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import reve_back.infrastructure.persistence.entity.BranchEntity;

public interface SpringDataBranchRepository extends JpaRepository<BranchEntity, Long> {
}
