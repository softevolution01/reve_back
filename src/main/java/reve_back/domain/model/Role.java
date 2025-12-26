package reve_back.domain.model;

import java.util.Set;

public record Role(
        Long id,
        String name,
        Set<Permission> permissions
) {
    public Role(String name, Set<Permission> permissions){
        this(null, name, permissions);
    }
}
