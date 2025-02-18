package app.g_agent.proposal_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.g_agent.proposal_service.model.Proposal;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    Optional<Proposal> findById(Long id);

    Optional<Proposal> findByIdAndCompanyId(Long id, Long companyId);

    Optional<List<Proposal>> findByCompanyId(Long companyId);
}