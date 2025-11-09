package reve_back.application.ports.in;

import reve_back.infrastructure.persistence.entity.BranchEntity;

import java.util.Set;

public record RegisterCommand(
        String username,
        String fullname,
        String email,
        String phone,
        String rawPassword,
        String rolName,
        Set<String> branchNames
) {
}
