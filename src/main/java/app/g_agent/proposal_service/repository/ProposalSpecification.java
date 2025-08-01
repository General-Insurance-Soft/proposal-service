package app.g_agent.proposal_service.repository;

import org.springframework.data.jpa.domain.Specification;

import app.g_agent.proposal_service.model.Proposal;

import jakarta.persistence.criteria.Predicate;

public class ProposalSpecification {

    public static Specification<Proposal> matchesKeyword(String keyword, int organizationId) {
        return (root, query, builder) -> {
            if ((keyword == null || keyword.trim().isEmpty())) {
                // Prevent returning all proposals
                return builder.disjunction();
            }

            Predicate orgPredicate = builder.equal(root.get("companyId"), organizationId);

            if (keyword == null || keyword.trim().isEmpty()) {
                return orgPredicate;
            }

            String likeKeyword = "%" + keyword.toLowerCase() + "%";
            Predicate keywordPredicate = builder.or(
                    builder.like(builder.lower(root.get("referenceNumber")), likeKeyword));

            return builder.and(orgPredicate, keywordPredicate);
        };
    }
}
