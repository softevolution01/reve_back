package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import reve_back.infrastructure.persistence.entity.ProductEntity;

public interface SpringDataProductRepository extends JpaRepository<ProductEntity,Long> {

    Page<ProductEntity> findAll(Pageable pageable);
}
