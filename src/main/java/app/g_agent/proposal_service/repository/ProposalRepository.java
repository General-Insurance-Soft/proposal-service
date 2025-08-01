package app.g_agent.proposal_service.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.g_agent.proposal_service.model.Proposal;

public interface ProposalRepository extends JpaRepository<Proposal, Long>, JpaSpecificationExecutor<Proposal> {
    Optional<Proposal> findById(Long id);

    Optional<Proposal> findByIdAndCompanyId(Long id, Long companyId);

    Page<Proposal> findByCompanyId(Pageable pageable, Long companyId);

    List<Proposal> findByContactIdAndCompanyId(Long contactId, Long companyId);

    @Query("SELECT DISTINCT p FROM Proposal p LEFT JOIN FETCH p.proposalDocuments WHERE p.id IN :ids")
    List<Proposal> findAllWithDocumentsByIds(@Param("ids") Set<Long> ids);
    @Query(value = """
            SELECT * FROM (
                SELECT p.*
                     , sub.proposal_count
                FROM (
                    SELECT *,
                           ROW_NUMBER() OVER (PARTITION BY contact_id ORDER BY created_at DESC) AS row_num
                    FROM proposal
                    WHERE company_id = :companyId
                ) p
                JOIN (
                    SELECT contact_id, COUNT(*) AS proposal_count
                    FROM proposal
                    WHERE company_id = :companyId
                    GROUP BY contact_id
                ) sub ON p.contact_id = sub.contact_id
                WHERE p.row_num = 1
            ) result
            """, countQuery = """
            SELECT COUNT(*) FROM (
                SELECT contact_id
                FROM proposal
                WHERE company_id = :companyId
                GROUP BY contact_id
            ) AS count_table
            """, nativeQuery = true)
    Page<Object[]> findLatestProposalsPerContact(Pageable pageable, @Param("companyId") Long companyId);

}