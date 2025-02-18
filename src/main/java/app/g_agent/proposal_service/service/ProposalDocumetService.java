package app.g_agent.proposal_service.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.g_agent.proposal_service.dto.ProposalDocumentDto;
import app.g_agent.proposal_service.model.ProposalDocument;
import app.g_agent.proposal_service.repository.ProposalDocumentRepository;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class ProposalDocumetService {

    private static final Logger logger = LoggerFactory.getLogger(ProposalService.class);


    private ProposalDocumentRepository proposalDocumentRepository;
    private JwtService jwtService;

    public ProposalDocumetService(ProposalDocumentRepository proposalDocumentRepository, JwtService jwtService) {
       
        this.proposalDocumentRepository = proposalDocumentRepository;
        this.jwtService = jwtService;
    }



    @Transactional
    public void createProposalDocument(HttpServletRequest request, ProposalDocumentDto proposalDocumentDto) throws Exception {
        ProposalDocument proposalDocument = new ProposalDocument();

        int userId = (int) jwtService.getTokenValue(jwtService.getJWT(request), "user-id");

        proposalDocument.setFolderName(proposalDocumentDto.getFolderName());
        proposalDocument.setDocumentName(proposalDocumentDto.getDocumentName());
        proposalDocument.setBlobUrl(proposalDocumentDto.getBlobUrl());
        proposalDocument.setUpdatedBy(Long.valueOf(userId));

        try {
            proposalDocumentRepository.save(proposalDocument);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                logger.info("ProposalDocument error ==========> id: " + ex.getMessage());
                throw new Exception("This proposal document already exists.");
            }
            throw ex; // Rethrow if not related to constraint violation
        }
    }

    @Transactional
    public void updateProposalDocument(HttpServletRequest request, ProposalDocumentDto proposalDocumentDto, Long id) throws Exception {
        Optional<ProposalDocument> proposalDocumentOpt = proposalDocumentRepository.findById(id);

        if (proposalDocumentOpt.isEmpty()) {
            throw new Exception("The proposal document cannot be found");
        }

        ProposalDocument proposalDocument = proposalDocumentOpt.get();

        int userId = (int) jwtService.getTokenValue(jwtService.getJWT(request), "user-id");

        proposalDocument.setFolderName(proposalDocumentDto.getFolderName());
        proposalDocument.setDocumentName(proposalDocumentDto.getDocumentName());
        proposalDocument.setBlobUrl(proposalDocumentDto.getBlobUrl());
        proposalDocument.setUpdatedBy(Long.valueOf(userId));

        try {
            proposalDocumentRepository.save(proposalDocument);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                logger.info("ProposalDocument error ==========> id: " + ex.getMessage());
                throw new Exception("This proposal document already exists.");
            }
            throw ex; // Rethrow if not related to constraint violation
        }
    }

    @Transactional
    public void deleteProposalDocument(HttpServletRequest request, Long id) throws Exception {
        Optional<ProposalDocument> proposalDocumentOpt = proposalDocumentRepository.findById(id);

        if (proposalDocumentOpt.isPresent()) {
            proposalDocumentRepository.delete(proposalDocumentOpt.get());
        } else {
            throw new Exception("The proposal document cannot be found");
        }
    }

    public ProposalDocumentDto getProposalDocument(HttpServletRequest request, Long id) throws Exception {
        Optional<ProposalDocument> proposalDocumentOpt = proposalDocumentRepository.findById(id);

        if (proposalDocumentOpt.isPresent()) {
            ProposalDocumentDto proposalDocumentDto = new ProposalDocumentDto();
            proposalDocumentDto.setId(proposalDocumentOpt.get().getId());
            proposalDocumentDto.setFolderName(proposalDocumentOpt.get().getFolderName());
            proposalDocumentDto.setDocumentName(proposalDocumentOpt.get().getDocumentName());
            proposalDocumentDto.setBlobUrl(proposalDocumentOpt.get().getBlobUrl());
            proposalDocumentDto.setUpdatedBy(proposalDocumentOpt.get().getUpdatedBy());
            proposalDocumentDto.setCreatedAt(proposalDocumentOpt.get().getCreatedAt());
            proposalDocumentDto.setUploadedAt(proposalDocumentOpt.get().getUploadedAt());

            return proposalDocumentDto;
        } else {
            throw new Exception("The proposal document does not exist");
        }
    }
}