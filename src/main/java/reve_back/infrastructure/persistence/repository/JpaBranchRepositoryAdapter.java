package reve_back.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reve_back.application.ports.out.BranchRepositoryPort;
import reve_back.domain.model.Branch;
import reve_back.infrastructure.persistence.jpa.SpringDataBranchRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class JpaBranchRepositoryAdapter implements BranchRepositoryPort {

    private final SpringDataBranchRepository springDataBranchRepository;

    @Override
    public List<Branch> findAll() {
        return springDataBranchRepository.findAll().stream()
                .map(e->new Branch(
                        e.getId(),
                        e.getName(),
                        e.getLocation()))
                .collect(Collectors.toList());
    }

    @Override
    public Set<Branch> findByNames(Set<String> names) {
        return springDataBranchRepository.findAllByNameIn(names).stream()
                .map(e -> new Branch(
                        e.getId(),
                        e.getName(),
                        e.getLocation()))
                .collect(Collectors.toSet());
    }
}
