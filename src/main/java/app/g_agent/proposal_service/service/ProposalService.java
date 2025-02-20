package app.g_agent.proposal_service.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.g_agent.proposal_service.dto.ProposalDocumentDto;
import app.g_agent.proposal_service.dto.ProposalDto;
import app.g_agent.proposal_service.model.Proposal;
import app.g_agent.proposal_service.model.ProposalDocument;
import app.g_agent.proposal_service.repository.ProposalDocumentRepository;
import app.g_agent.proposal_service.repository.ProposalRepository;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class ProposalService {

    private static final Logger logger = LoggerFactory.getLogger(ProposalService.class);

    private ProposalRepository proposalRepository;
    private ProposalDocumentRepository proposalDocumentRepository;
    private JwtService jwtService;

    public ProposalService(ProposalRepository proposalRepository, ProposalDocumentRepository proposalDocumentRepository,
            JwtService jwtService) {
        this.proposalRepository = proposalRepository;
        this.proposalDocumentRepository = proposalDocumentRepository;
        this.jwtService = jwtService;
    }

    public Proposal getProposalById(Long id) throws Exception {
        Optional<Proposal> proposal = proposalRepository.findById(id);
        if (proposal.isPresent()) {
            return proposal.get();
        } else {
            throw new Exception("The Proposal does not exist");
        }
    }

    @Transactional
    public void deleteProposal(HttpServletRequest request, Long id) throws Exception {
        Optional<Proposal> proposalOpt = proposalRepository.findById(id);

        if (proposalOpt.isPresent()) {
            proposalRepository.delete(proposalOpt.get());
        } else {
            throw new Exception("The proposal cannot be found");
        }
    }

    public ProposalDto getProposal(HttpServletRequest request, Long id) throws Exception {
        Optional<Proposal> proposalOpt = proposalRepository.findById(id);

        if (proposalOpt.isPresent()) {
            Proposal proposal = proposalOpt.get();
            ProposalDto proposalDto = new ProposalDto();
            proposalDto.setId(proposal.getId());
            proposalDto.setInsuranceCompanyId(proposal.getInsuranceCompanyId());
            proposalDto.setPolicyTypeId(proposal.getPolicyTypeId());
            proposalDto.setStartDate(proposal.getStartDate());
            proposalDto.setEndDate(proposal.getEndDate());
            proposalDto.setCompanyId(proposal.getCompanyId());
            proposalDto.setContactId(proposal.getContactId());
            proposalDto.setUpdatedBy(proposal.getUpdatedBy());
            proposalDto.setCreatedAt(proposal.getCreatedAt());
            proposalDto.setUpdatedAt(proposal.getUpdatedAt());

            Set<ProposalDocumentDto> proposalDocumentDtos = proposal.getProposalDocuments().stream().map(document -> {
                ProposalDocumentDto documentDto = new ProposalDocumentDto();
                documentDto.setId(document.getId());
                documentDto.setFolderName(document.getFolderName());
                documentDto.setDocumentName(document.getDocumentName());
                documentDto.setBlobUrl(document.getBlobUrl());
                documentDto.setUpdatedBy(document.getUpdatedBy());
                documentDto.setCreatedAt(document.getCreatedAt());
                return documentDto;
            }).collect(Collectors.toSet());

            proposalDto.setProposalDocuments(proposalDocumentDtos);

            return proposalDto;
        } else {
            throw new Exception("The proposal does not exist");
        }
    }

    @Transactional
    public void createProposal(HttpServletRequest request, ProposalDto proposalDto) throws Exception {
        Proposal proposal = new Proposal();

        Long userId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "user-id").toString());
        Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());
        logger.info("user ID: ==============================>" + userId);
        proposal.setInsuranceCompanyId(proposalDto.getInsuranceCompanyId());
        proposal.setPolicyTypeId(proposalDto.getPolicyTypeId());
        proposal.setStartDate(proposalDto.getStartDate());
        proposal.setEndDate(proposalDto.getEndDate());
        proposal.setCompanyId(orgId);
        proposal.setContactId(proposalDto.getContactId());
        proposal.setUpdatedBy(Long.valueOf(userId));

        if (proposalDto.getProposalDocuments() != null) {
            Set<ProposalDocument> proposalDocuments = new HashSet<>();
            proposalDto.getProposalDocuments().forEach(documentDto -> {
                ProposalDocument document = new ProposalDocument();
                document.setFolderName(documentDto.getFolderName());
                document.setDocumentName(documentDto.getDocumentName());
                document.setBlobUrl(documentDto.getBlobUrl());
                document.setUpdatedBy(Long.valueOf(userId));
                document.setProposal(proposal); // Set the proposal reference
                proposalDocuments.add(document);
            });
            proposal.setProposalDocuments(proposalDocuments);
        }

        try {
            proposalRepository.save(proposal);
            proposalDocumentRepository.saveAll(proposal.getProposalDocuments()); // Save the proposal documents
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                logger.info("Proposal error ==========> id: " + ex.getMessage());
                throw new Exception("This proposal already exists.");
            }
            throw ex; // Rethrow if not related to constraint violation
        }
    }

    @Transactional
    public void updateProposal(HttpServletRequest request, ProposalDto proposalDto, Long id) throws Exception {
        Optional<Proposal> proposalOpt = proposalRepository.findById(id);

        if (proposalOpt.isEmpty()) {
            throw new Exception("The proposal cannot be found");
        }

        Proposal proposal = proposalOpt.get();

        Long userId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "user-id").toString());

        proposal.setInsuranceCompanyId(proposalDto.getInsuranceCompanyId());
        proposal.setPolicyTypeId(proposalDto.getPolicyTypeId());
        proposal.setStartDate(proposalDto.getStartDate());
        proposal.setEndDate(proposalDto.getEndDate());
        proposal.setCompanyId(proposalDto.getCompanyId());
        proposal.setContactId(proposalDto.getContactId());
        proposal.setUpdatedBy(Long.valueOf(userId));

        if (proposalDto.getProposalDocuments() != null) {
            Set<ProposalDocument> proposalDocuments = new HashSet<>();
            proposalDto.getProposalDocuments().forEach(documentDto -> {
                ProposalDocument document = new ProposalDocument();
                document.setFolderName(documentDto.getFolderName());
                document.setDocumentName(documentDto.getDocumentName());
                document.setBlobUrl(documentDto.getBlobUrl());
                document.setUpdatedBy(Long.valueOf(userId));
                document.setProposal(proposal); // Set the proposal reference
                proposalDocuments.add(document);
            });
            proposal.setProposalDocuments(proposalDocuments);
        }

        try {
            proposalRepository.save(proposal);
            proposalDocumentRepository.saveAll(proposal.getProposalDocuments()); // Save the proposal documents
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                logger.info("Proposal error ==========> id: " + ex.getMessage());
                throw new Exception("This proposal already exists.");
            }
            throw ex; // Rethrow if not related to constraint violation
        }
    }

    @Transactional
    public List<ProposalDto> getProposals(HttpServletRequest request) throws Exception {
        List<Proposal> proposals = proposalRepository.findAll();

        return proposals.stream().map(proposal -> {
            ProposalDto proposalDto = new ProposalDto();
            proposalDto.setId(proposal.getId());
            proposalDto.setInsuranceCompanyId(proposal.getInsuranceCompanyId());
            proposalDto.setPolicyTypeId(proposal.getPolicyTypeId());
            proposalDto.setStartDate(proposal.getStartDate());
            proposalDto.setEndDate(proposal.getEndDate());
            proposalDto.setCompanyId(proposal.getCompanyId());
            proposalDto.setContactId(proposal.getContactId());
            proposalDto.setUpdatedBy(proposal.getUpdatedBy());
            proposalDto.setCreatedAt(proposal.getCreatedAt());
            proposalDto.setUpdatedAt(proposal.getUpdatedAt());

            Set<ProposalDocumentDto> proposalDocumentDtos = proposal.getProposalDocuments().stream().map(document -> {
                ProposalDocumentDto documentDto = new ProposalDocumentDto();
                documentDto.setId(document.getId());
                documentDto.setFolderName(document.getFolderName());
                documentDto.setDocumentName(document.getDocumentName());
                documentDto.setBlobUrl(document.getBlobUrl());
                documentDto.setUpdatedBy(document.getUpdatedBy());
                documentDto.setCreatedAt(document.getCreatedAt());
                return documentDto;
            }).collect(Collectors.toSet());

            proposalDto.setProposalDocuments(proposalDocumentDtos);

            return proposalDto;
        }).collect(Collectors.toList());
    }

}