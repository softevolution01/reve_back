package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.BranchRepositoryPort;
import reve_back.domain.model.Branch;
import reve_back.infrastructure.persistence.jpa.SpringDataBranchRepository;
import reve_back.infrastructure.persistence.mapper.PersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class JpaBranchRepositoryAdapter implements BranchRepositoryPort {

    private final SpringDataBranchRepository springDataBranchRepository;
    private final PersistenceMapper mapper;

    @Override
    public List<Branch> findAll() {
        return springDataBranchRepository.findAll().stream()
                .map(e->new Branch(
                        e.getId(),
                        e.getName(),
                        e.getLocation(),
                        e.getWarehouse().getId(),
                        e.getIsCashManagedCentralized()))
                .collect(Collectors.toList());
    }

    @Override
    public Set<Branch> findByNames(Set<String> names) {
        return springDataBranchRepository.findAllByNameIn(names).stream()
                .map(e -> new Branch(
                        e.getId(),
                        e.getName(),
                        e.getLocation(),
                        e.getWarehouse().getId(),
                        e.getIsCashManagedCentralized()))
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<Branch> findById(Long id) {
        return springDataBranchRepository.findById(id)
                .map(mapper::toDomain);
    }
}
