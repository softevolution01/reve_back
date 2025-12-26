package reve_back.domain.model;

import java.time.LocalDateTime;

public record Client(
        Long id,
        String fullname,
        String dni,
        String email,
        String phone,
        Boolean isVip,           // boolean -> Boolean
        LocalDateTime vipSince,
        Integer vipPurchaseCounter, // int -> Integer
        LocalDateTime createdAt
) {
}
