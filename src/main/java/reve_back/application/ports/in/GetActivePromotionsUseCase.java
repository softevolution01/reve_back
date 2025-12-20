package reve_back.application.ports.in;

import reve_back.domain.model.Promotion;

import java.util.List;

public interface GetActivePromotionsUseCase {
    List<Promotion> execute();
}
