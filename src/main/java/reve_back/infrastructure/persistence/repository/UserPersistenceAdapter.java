package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reve_back.application.ports.out.UserRepositoryPort;
import reve_back.domain.model.Branch;
import reve_back.domain.model.Role;
import reve_back.domain.model.User;
import reve_back.infrastructure.persistence.entity.ClientEntity;
import reve_back.infrastructure.persistence.entity.UserEntity;
import reve_back.infrastructure.persistence.jpa.BranchJpaRepository;
import reve_back.infrastructure.persistence.jpa.RoleJpaRepository;
import reve_back.infrastructure.persistence.jpa.UserJpaRepository;
import reve_back.infrastructure.persistence.mapper.PersistenceMapper;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository userJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final BranchJpaRepository branchJpaRepository;
    private final PersistenceMapper mapper;

    @Override
    public User save(User user) {
        UserEntity userEntity = mapper.toEntity(user);
        if (user.roles() != null && !user.roles().isEmpty()) {
            Set<String> roleNames = user.roles().stream()
                    .map(Role::name)// cambio de role -> role.name()
                    .collect(Collectors.toSet());

            userEntity.setRoles(
                    roleNames.stream()
                            .map(name -> roleJpaRepository.findByName(name)
                                    .orElseThrow(()-> new RuntimeException("Rol no encontrado: " + name)))
                            .collect(Collectors.toSet())
            );
        } else {
            userEntity.setRoles(new HashSet<>());
        }

        if (user.branches() != null && !user.branches().isEmpty()) {
            Set<String> branchNames = user.branches().stream().map(Branch::name).collect(Collectors.toSet());
            userEntity.setBranches(branchJpaRepository.findAllByNameIn(branchNames));
        } else {
            userEntity.setBranches(new HashSet<>());
        }

        if (user.clientId() != null) {
            ClientEntity client = new ClientEntity();
            client.setId(user.clientId());
            userEntity.setClient(client);
        }else{
            userEntity.setClient(null);
        }

        UserEntity savedEntity = userJpaRepository.save(userEntity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userJpaRepository.findByUsername(username)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userJpaRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id)
                .map(mapper::toDomain);

    }
}
