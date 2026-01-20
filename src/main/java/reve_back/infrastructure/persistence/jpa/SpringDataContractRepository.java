package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reve_back.infrastructure.persistence.entity.ContractEntity;

@RepositoryRestResource(exported = false)
public interface SpringDataContractRepository extends JpaRepository<ContractEntity, Long> {
    @Query("SELECT c FROM ContractEntity c ORDER BY c.createdAt DESC")
    Page<ContractEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
