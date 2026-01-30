package reve_back.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reve_back.infrastructure.persistence.entity.CashSessionEntity;

import java.util.Optional;

@Repository
public interface SpringDataCashSessionRepository extends JpaRepository<CashSessionEntity, Long> {

    // 1. Buscar la sesión abierta de un almacén específico
    // Asumimos que el estado en la base de datos se guarda como String o Enum (OPEN/CLOSED)
    @Query("SELECT s FROM CashSessionEntity s WHERE s.warehouse.id = :warehouseId AND s.status = 'OPEN'")
    Optional<CashSessionEntity> findOpenByWarehouse(@Param("warehouseId") Long warehouseId);

    // CORRECCIÓN: Usamos 's.warehouse.id' aquí también
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM CashSessionEntity s WHERE s.warehouse.id = :warehouseId AND s.status = 'OPEN'")
    boolean existsOpenByWarehouse(@Param("warehouseId") Long warehouseId);

}
