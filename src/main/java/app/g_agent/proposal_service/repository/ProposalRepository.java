package app.g_agent.proposal_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import app.g_agent.proposal_service.model.Proposal;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    Optional<Proposal> findById(Long id);

    Optional<Proposal> findByIdAndCompanyId(Long id, Long companyId);

    Page<Proposal> findByCompanyId(Pageable pageable, Long companyId);

    List<Proposal> findByContactIdAndCompanyId(Long contactId, Long companyId);
}