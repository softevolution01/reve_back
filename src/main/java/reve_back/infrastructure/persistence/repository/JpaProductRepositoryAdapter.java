package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.ProductRepositoryPort;
import reve_back.domain.model.Product;
import reve_back.infrastructure.persistence.entity.ProductEntity;
import reve_back.infrastructure.persistence.jpa.SpringDataProductRepository;
import reve_back.infrastructure.persistence.mapper.PersistenceMapper;
import reve_back.infrastructure.web.dto.LabelItemDTO;
import reve_back.infrastructure.web.dto.ProductSummaryDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class JpaProductRepositoryAdapter implements ProductRepositoryPort {

    private final SpringDataProductRepository springDataProductRepository;
    private final PersistenceMapper mapper;

    @Override
    public Product save(Product product) {
        ProductEntity entity = mapper.toEntity(product);
        ProductEntity savedEntity = springDataProductRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Page<ProductSummaryDTO> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductEntity> productPage = springDataProductRepository.findByIsActiveTrue(pageable);
        return productPage.map(mapper::toSummaryDto);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return springDataProductRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public void setInactiveById(Long id) {
        springDataProductRepository.findById(id).ifPresent(entity -> {
            entity.setActive(false);
            springDataProductRepository.save(entity);
        });
    }

    @Override
    public boolean existsByBrandAndLineAndConcentrationAndVolumeProductsMl(String brand, String line, String concentration, Integer unitVolumeMl) {
        return springDataProductRepository.existsByBrandIgnoreCaseAndLineIgnoreCaseAndConcentrationIgnoreCaseAndVolumeProductsMl(brand, line, concentration, unitVolumeMl);
    }

    @Override
    public boolean existsByBrandAndLineAndConcentrationAndVolumeProductsMlAndIdNot(String brand, String line, String concentration, Integer unitVolumeMl, Long id) {
        return springDataProductRepository.existsByBrandIgnoreCaseAndLineIgnoreCaseAndConcentrationIgnoreCaseAndVolumeProductsMlAndIdNot(brand, line, concentration, unitVolumeMl, id);
    }

    @Override
    public List<LabelItemDTO> getLabelCatalog() {
        // 1. Obtener datos crudos (Maps) de Spring Data
        List<Map<String, Object>> rawRows = springDataProductRepository.findAllLabelItemsRaw();

        // 2. Mapear manualmente a tu Record
        return rawRows.stream().map(row -> {

            String nombre = (String) row.get("nombre_completo");
            String detalle = (String) row.get("detalle");
            String codigo = (String) row.get("codigo_visual");

            // Conversión segura de precio (Postgres puede devolver Double o BigDecimal)
            Object precioObj = row.get("precio");
            BigDecimal precio = BigDecimal.ZERO;

            if (precioObj != null) {
                // Convertir a String primero es la forma más segura de crear un BigDecimal
                // desde cualquier tipo numérico (Double, Integer, etc.)
                precio = new BigDecimal(precioObj.toString());
            }

            // Retornamos el Record
            return new LabelItemDTO(nombre, detalle, codigo, precio);

        }).toList();
    }

    @Override
    public Page<ProductSummaryDTO> searchByBrandOrLine(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductEntity> entityPage = springDataProductRepository.searchByQuery(query, pageable);
        return entityPage.map(mapper::toSummaryDto);
    }
}
