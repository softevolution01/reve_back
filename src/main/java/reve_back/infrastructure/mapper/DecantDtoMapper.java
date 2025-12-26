package reve_back.infrastructure.mapper;

import org.springframework.stereotype.Component;
import reve_back.domain.model.DecantPrice;
import reve_back.infrastructure.web.dto.DecantRequest;
import reve_back.infrastructure.web.dto.DecantResponse;

import java.util.List;

@Component
public class DecantDtoMapper {

    public DecantPrice toDomain(DecantRequest request) {
        return new DecantPrice(null, null, request.volumeMl(), request.price(), null, null);
    }

    public DecantResponse toResponse(DecantPrice domain) {
        if (domain == null) return null;

        return new DecantResponse(
                domain.id(),
                domain.volumeMl(),
                domain.price(),
                domain.barcode(),      // Incluimos el código
                domain.imageBarcode()  // Incluimos la URL de la imagen
        );
    }
}
