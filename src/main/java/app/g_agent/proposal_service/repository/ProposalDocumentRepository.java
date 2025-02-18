package app.g_agent.proposal_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.g_agent.proposal_service.model.ProposalDocument;

public interface ProposalDocumentRepository extends JpaRepository<ProposalDocument, Long> {
    Optional<ProposalDocument> findById(Long id);

    Optional<List<ProposalDocument>> findByProposalId(Long proposalId);
}