package reve_back.domain.model;

import java.time.LocalDateTime;
import java.util.Set;

public record User(
        Long id,
        String username,
        String fullname,
        String email,
        String phone,
        String passwordHash,
        Set<Role> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long clientId
) {
    public User(String username, String fullname, String email, String phone, String passwordHash, Set<Role> roles, Long clientId) {
        this(null,username, fullname, email, phone, passwordHash, roles, null, null, clientId);
    }
}
