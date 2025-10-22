package reve_back.application.ports.out;

import reve_back.domain.model.Branch;

import java.util.List;

public interface BranchRepositoryPort {
    List<Branch> findAll();
}
