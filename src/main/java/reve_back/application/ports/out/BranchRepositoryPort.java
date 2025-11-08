package reve_back.application.ports.out;

import reve_back.domain.model.Branch;

import java.util.List;
import java.util.Set;

public interface BranchRepositoryPort {
    List<Branch> findAll();
    Set<Branch> findByNames(Set<String> names);
}
