package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.LabelItemDTO;

import java.util.List;

public interface GetLabelCatalogUseCase {
    List<LabelItemDTO> execute();
}
