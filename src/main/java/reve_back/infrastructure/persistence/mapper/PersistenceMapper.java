package reve_back.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import reve_back.domain.model.*;
import reve_back.infrastructure.persistence.entity.*;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PersistenceMapper {

    public Branch toDomain(BranchEntity entity) {
        return new Branch(
                entity.getId(),
                entity.getName(),
                entity.getLocation()
        );
    }

    public Permission toDomain(PermissionEntity entity){
        return new Permission(entity.getId(),entity.getName());
    }

    public Role toDomain(RoleEntity entity){
        return new Role(
                entity.getId(),
                entity.getName(),
                entity.getPermissions().stream()
                        .map(this::toDomain)
                        .collect(Collectors.toSet())
        );
    }

    public User toDomain(UserEntity entity){
        Long cliendId = (entity.getClient() != null) ? entity.getClient().getId() : null;

        Set<Branch> branches = entity.getBranches().stream()
                .map(this::toDomain)
                .collect(Collectors.toSet());

        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getFullname(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getPasswordHash(),
                entity.getRoles().stream()
                        .map(this::toDomain)
                        .collect(Collectors.toSet()),
                branches,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                cliendId
        );
    }

    // --- Mapeo a Entidad ---
    public PermissionEntity toEntity(Permission domain){
        return null;
    }

    public RoleEntity toEntity(Role domain){
        return null;
    }

    public UserEntity toEntity(User domain){
        UserEntity entity = new UserEntity();
        entity.setId(domain.id());
        entity.setUsername(domain.username());
        entity.setFullname(domain.fullname());
        entity.setEmail(domain.email());
        entity.setPhone(domain.phone());
        entity.setPasswordHash(domain.passwordHash());
        return entity;
    }

    public Client toDomain(ClientEntity entity) {
        if (entity == null) return null;
        return new reve_back.domain.model.Client(
                entity.getId(),
                entity.getFullname(),
                entity.getDni(),
                entity.getEmail(),
                entity.getPhone(),
                entity.isVip(),
                entity.getVipSince(),
                entity.getVipPurchaseCounter(),
                entity.getCreatedAt()
        );
    }
}
