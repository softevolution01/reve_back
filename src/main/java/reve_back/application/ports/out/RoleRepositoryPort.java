package reve_back.application.ports.out;

import reve_back.domain.model.Role;

import java.util.Optional;

public interface RoleRepositoryPort {
    Optional<Role> findByName(String name);
}
