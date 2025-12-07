package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reve_back.infrastructure.persistence.entity.ClientEntity;

import java.util.List;

@RepositoryRestResource(exported = false)
public interface ClientJpaRepository extends JpaRepository<ClientEntity, Long> {
    @Query("SELECT c FROM ClientEntity c WHERE " +
            "LOWER(c.fullname) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "c.dni LIKE CONCAT('%', :query, '%')")
    List<ClientEntity> searchByFullnameOrDni(@Param("query") String query);

    boolean existsByDni(String dni);
    boolean existsByEmail(String email);
}
