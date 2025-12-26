package reve_back.domain.model;

import reve_back.infrastructure.web.dto.CartItemRequest;

import java.math.BigDecimal;
import java.util.List;

public interface PromotionStrategy {
    StrategyResult execute(List<CartItem> items, int n, int m);
    String getStrategyCode();
}
