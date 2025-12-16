package reve_back.domain.model;

import java.time.LocalDateTime;

public record Client(
        Long id,
        String fullname,
        String dni,
        String email,
        String phone,
        boolean isVip,
        LocalDateTime vipSince,
        int vipPurchaseCounter,
        LocalDateTime createdAt
) {
}
