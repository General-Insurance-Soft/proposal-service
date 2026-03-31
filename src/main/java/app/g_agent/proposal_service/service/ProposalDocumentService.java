package app.g_agent.proposal_service.service;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import app.g_agent.proposal_service.dto.ProposalDocumentDto;
import app.g_agent.proposal_service.model.ProposalDocument;
import app.g_agent.proposal_service.repository.ProposalDocumentRepository;
import jakarta.servlet.http.HttpServletRequest;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class ProposalDocumentService {

    @Value("${backblaze.s3.bucket-name}")
    private String bucketName;

    @Autowired
    B2ClientFactory b2ClientFactory;

    private static final Logger logger = LoggerFactory.getLogger(ProposalService.class);

    private ProposalDocumentRepository proposalDocumentRepository;
    private JwtService jwtService;

    public ProposalDocumentService(ProposalDocumentRepository proposalDocumentRepository, JwtService jwtService) {
        this.proposalDocumentRepository = proposalDocumentRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public void createProposalDocument(HttpServletRequest request, ProposalDocumentDto proposalDocumentDto)
            throws Exception {
        ProposalDocument proposalDocument = new ProposalDocument();

        int userId = (int) jwtService.getTokenValue(jwtService.getJWT(request), "user-id");

        proposalDocument.setFolderName(proposalDocumentDto.getFolderName());
        proposalDocument.setDocumentName(proposalDocumentDto.getDocumentName());
        proposalDocument.setBlobUrl(proposalDocumentDto.getBlobUrl());
        proposalDocument.setUpdatedBy(Long.valueOf(userId));
        proposalDocument.setDocumentType(proposalDocumentDto.getDocumentType());

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
    public void updateProposalDocument(HttpServletRequest request, ProposalDocumentDto proposalDocumentDto, Long id)
            throws Exception {
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
        proposalDocument.setDocumentType(proposalDocumentDto.getDocumentType());

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
            proposalDocumentDto.setProposalId(proposalDocumentOpt.get().getProposal().getId());
            proposalDocumentDto.setFolderName(proposalDocumentOpt.get().getFolderName());
            proposalDocumentDto.setDocumentName(proposalDocumentOpt.get().getDocumentName());
            proposalDocumentDto.setBlobUrl(proposalDocumentOpt.get().getBlobUrl());
            proposalDocumentDto.setDocumentType(proposalDocumentOpt.get().getDocumentType());
            proposalDocumentDto.setUpdatedBy(proposalDocumentOpt.get().getUpdatedBy());
            proposalDocumentDto.setCreatedAt(proposalDocumentOpt.get().getCreatedAt());

            return proposalDocumentDto;
        } else {
            throw new Exception("The proposal document does not exist");
        }
    }

    public List<ProposalDocumentDto> getProposalDocuments(HttpServletRequest request) throws Exception {
        List<ProposalDocument> proposalDocuments = proposalDocumentRepository.findAll();

        return proposalDocuments.stream().map(proposalDocument -> {
            ProposalDocumentDto proposalDocumentDto = new ProposalDocumentDto();
            proposalDocumentDto.setId(proposalDocument.getId());
            proposalDocumentDto.setProposalId(proposalDocument.getProposal().getId());
            proposalDocumentDto.setFolderName(proposalDocument.getFolderName());
            proposalDocumentDto.setDocumentName(proposalDocument.getDocumentName());
            proposalDocumentDto.setBlobUrl(proposalDocument.getBlobUrl());
            proposalDocumentDto.setDocumentType(proposalDocument.getDocumentType());
            proposalDocumentDto.setUpdatedBy(proposalDocument.getUpdatedBy());
            proposalDocumentDto.setCreatedAt(proposalDocument.getCreatedAt());
            return proposalDocumentDto;
        }).collect(Collectors.toList());
    }

    public boolean deleteProposalDocumentFile(HttpServletRequest request, Long id) {

        // https://s3.<your-region>.backblazeb2.com/<your-bucket-name>/<your-key>
        // https://s3.us-east-005.backblazeb2.com/gisca-store/2/proposal-service/three-160935-1774934871712.jpeg
        Optional<ProposalDocument> proposalDocumentOpt = proposalDocumentRepository.findById(id);

        try {

            S3Client s3 = b2ClientFactory.createClient();

            String bucketUrl = proposalDocumentOpt.get().getBlobUrl();
            
            String path = bucketUrl.substring(bucketUrl.indexOf(".com/") + 5);
            int firstSlash = path.indexOf('/');
            String key = path.substring(firstSlash + 1);

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3.deleteObject(deleteRequest);

        } catch (Exception e) {
            logger.error("Error uploading file to S3: " + e.getMessage());
            throw new RuntimeException(e);
        }

        return true;

    }
}