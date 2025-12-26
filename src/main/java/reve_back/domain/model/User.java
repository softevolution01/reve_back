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
        Set<Branch> branches,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long clientId
) {
    public User(String username, String fullname, String email, String phone, String passwordHash, Set<Role> roles, Set<Branch> branches, Long clientId) {
        this(null,username, fullname, email, phone, passwordHash, roles, branches, null, null, clientId);
    }
}
