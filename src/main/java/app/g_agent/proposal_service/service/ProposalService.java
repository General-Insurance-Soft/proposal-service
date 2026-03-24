package app.g_agent.proposal_service.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import app.g_agent.proposal_service.commons.Message;
import app.g_agent.proposal_service.dto.ContactAddressWrapper;
import app.g_agent.proposal_service.dto.ContactDto;
import app.g_agent.proposal_service.dto.LocalityDto;
import app.g_agent.proposal_service.dto.ProposalDocumentDto;
import app.g_agent.proposal_service.dto.ProposalDto;
import app.g_agent.proposal_service.model.Proposal;
import app.g_agent.proposal_service.model.ProposalDocument;
import app.g_agent.proposal_service.repository.ProposalDocumentRepository;
import app.g_agent.proposal_service.repository.ProposalRepository;
import app.g_agent.proposal_service.system.exception.DuplicateContactException;
import jakarta.servlet.http.HttpServletRequest;
import app.g_agent.proposal_service.dto.ProposalSaveResponse;
import app.g_agent.proposal_service.dto.ProposalSearchResultDto;
import app.g_agent.proposal_service.dto.UserDto;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import com.fasterxml.jackson.core.type.TypeReference;

//ProposalSpecification
import app.g_agent.proposal_service.repository.ProposalSpecification;

@Service
public class ProposalService {

    private static final Logger logger = LoggerFactory.getLogger(ProposalService.class);

    @Autowired
    private ContactsDataClient contactsDataClient;
    @Autowired
    private UsersDataClient usersDataClient;
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
        Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());
        Optional<Proposal> proposalOpt = proposalRepository.findByIdAndCompanyId(id, orgId);

        if (proposalOpt.isPresent()) {
            proposalRepository.delete(proposalOpt.get());
        } else {
            throw new Exception("The proposal cannot be found");
        }
    }

    public ProposalDto getProposal(HttpServletRequest request, Long id) throws Exception {
        Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());

        Optional<Proposal> proposalOpt = proposalRepository.findByIdAndCompanyId(id, orgId);

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
            proposalDto.setReferenceNumber(proposal.getReferenceNumber());

            Set<ProposalDocumentDto> proposalDocumentDtos = proposal.getProposalDocuments().stream().map(document -> {
                ProposalDocumentDto documentDto = new ProposalDocumentDto();
                documentDto.setId(document.getId());
                documentDto.setFolderName(document.getFolderName());
                documentDto.setDocumentName(document.getDocumentName());
                documentDto.setBlobUrl(document.getBlobUrl());
                documentDto.setDocumentType(document.getDocumentType());
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
    public ProposalSaveResponse createProposal(HttpServletRequest request, ProposalDto proposalDto) throws Exception {
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
        proposal.setReferenceNumber(proposalDto.getReferenceNumber());

        if (proposalDto.getProposalDocuments() != null) {
            logger.info("proposalDto.getProposalDocuments() not null");
            Set<ProposalDocument> proposalDocuments = new HashSet<>();
            proposalDto.getProposalDocuments().forEach(documentDto -> {
                logger.debug("Document name: " + documentDto.getDocumentName());
                logger.debug("blob url: " + documentDto.getBlobUrl());
                ProposalDocument document = new ProposalDocument();
                document.setFolderName(documentDto.getFolderName());
                document.setDocumentName(documentDto.getDocumentName());
                document.setBlobUrl(documentDto.getBlobUrl());
                document.setDocumentType(documentDto.getDocumentType());
                document.setUpdatedBy(Long.valueOf(userId));
                document.setProposal(proposal); // Set the proposal reference
                proposalDocuments.add(document);
            });
            proposal.setProposalDocuments(proposalDocuments);
        }

        try {
            proposalRepository.save(proposal);
            proposalDocumentRepository.saveAll(proposal.getProposalDocuments()); // Save the proposal documents
            return new ProposalSaveResponse() {
                {
                    setProposalId(proposal.getId().toString());
                }
            };
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                logger.info("Proposal error ==========> id: " + ex.getMessage());
                throw new DuplicateContactException("This proposal already exists.");
            }
            throw ex; // Rethrow if not related to constraint violation
        }
    }

    @Transactional
    public ResponseEntity<?> updateProposal(HttpServletRequest request, ProposalDto proposalDto, Long id)
            throws Exception {

        Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());
        Optional<Proposal> proposalOpt = proposalRepository.findByIdAndCompanyId(id, orgId);

        if (proposalOpt.isEmpty()) {
            // throw new Exception("The proposal cannot be found");
            Message message = new Message();
            message.setMessage("The proposal cannot be found");
            return ResponseEntity.status(501).body(message);
        }

        Proposal proposal = proposalOpt.get();

        Long userId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "user-id").toString());

        proposal.setInsuranceCompanyId(proposalDto.getInsuranceCompanyId());
        proposal.setPolicyTypeId(proposalDto.getPolicyTypeId());
        proposal.setStartDate(proposalDto.getStartDate());
        proposal.setEndDate(proposalDto.getEndDate());
        proposal.setUpdatedBy(Long.valueOf(userId));
        proposal.setReferenceNumber(proposalDto.getReferenceNumber());

        try {
            proposalRepository.save(proposal);
            Message message = new Message();
            message.setMessage("Proposal updated successfully");
            return ResponseEntity.status(200).body(message);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                logger.info("Proposal error ==========> id: " + ex.getMessage());
                Message message = new Message();
                message.setMessage("This proposal already exists.");
                return ResponseEntity.status(409).body(message);
            }
            throw ex; // Rethrow if not related to constraint violation
        }
    }

    @Transactional
    public Map<String, Object> getProposals(HttpServletRequest request, MultiValueMap<String, String> headers, int page,
            int size) throws Exception {
        // List<Proposal> proposals = proposalRepository.findAll();
        Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());
        Pageable pageable = PageRequest.of(page, size, Sort.by("created_at").descending());
        Page<Object[]> proposalPage = proposalRepository.findLatestProposalsPerContact(pageable, orgId);

        Set<Long> contacts = new HashSet<Long>();
        Set<Long> updatedByUsers = new HashSet<Long>();

        List<ProposalDto> dtoList = new ArrayList<>();
        for (Object[] row : proposalPage.getContent()) {
            ProposalDto proposalDto = new ProposalDto();
            proposalDto.setId((Long) row[5]);
            proposalDto.setInsuranceCompanyId((Long) row[6]);
            proposalDto.setPolicyTypeId((Long) row[7]);
            // proposalDto.setStartDate(row[1]);
            proposalDto.setStartDate(((java.sql.Date) row[1]).toLocalDate());

            proposalDto.setEndDate(((java.sql.Date) row[0]).toLocalDate());
            proposalDto.setCompanyId((Long) row[2]);
            proposalDto.setContactId((Long) row[3]);
            proposalDto.setUpdatedBy((Long) row[9]);

            proposalDto.setCreatedAt(((java.sql.Timestamp) row[4]).toLocalDateTime());
            proposalDto.setUpdatedAt(((java.sql.Timestamp) row[8]).toLocalDateTime());
            proposalDto.setReferenceNumber((String) row[10]);

            contacts.add((Long) row[3]);
            updatedByUsers.add((Long) row[9]);

            Long proposalCount = ((Number) row[row.length - 1]).longValue(); // last element
            proposalDto.setProposalCount(proposalCount);

            dtoList.add(proposalDto);
        }

        // start fetch documents
        Set<Long> proposalIds = dtoList.stream().map(ProposalDto::getId).collect(Collectors.toSet());
        List<Proposal> proposalsWithDocs = proposalRepository.findAllWithDocumentsByIds(proposalIds);

        Map<Long, Set<ProposalDocumentDto>> proposalDocsMap = proposalsWithDocs.stream()
                .collect(Collectors.toMap(
                        Proposal::getId,
                        proposal -> proposal.getProposalDocuments().stream().map(document -> {
                            ProposalDocumentDto docDto = new ProposalDocumentDto();
                            docDto.setId(document.getId());
                            docDto.setFolderName(document.getFolderName());
                            docDto.setDocumentName(document.getDocumentName());
                            docDto.setBlobUrl(document.getBlobUrl());
                            docDto.setDocumentType(document.getDocumentType());
                            docDto.setUpdatedBy(document.getUpdatedBy());
                            docDto.setCreatedAt(document.getCreatedAt());
                            return docDto;
                        }).collect(Collectors.toSet())));

        dtoList.forEach(dto -> {
            dto.setProposalDocuments(proposalDocsMap.getOrDefault(dto.getId(), Collections.emptySet()));
        });
        // end fetch documents

        Page<ProposalDto> proposals = new PageImpl<>(dtoList, proposalPage.getPageable(),
                proposalPage.getTotalElements());

        ContactAddressWrapper contactsData = getAdministrativeAreasData(contacts, headers);
        List<UserDto> updatedByUsersData = getUpdatedByData(updatedByUsers, headers);

        Map<String, Object> response = new HashMap<>();

        response.put("totalElements", proposals.getTotalElements());
        response.put("totalPages", proposals.getTotalPages());
        response.put("currentPage", proposals.getNumber());

        response.put("proposals", proposals.getContent());
        response.put("contact", contactsData.contacts);
        response.put("updatedBy", updatedByUsersData);
        response.put("administrative_areas", contactsData.localityMapper);

        return response;

    }

    private ContactAddressWrapper getAdministrativeAreasData(Set<Long> ids,
            MultiValueMap<String, String> headers) {
        logger.info("Number of admin area IDs============>: {}", ids.size());

        headers.add("Content-Type", "application/json");

        Map<String, String> flattenedHeaders = new HashMap<>();
        headers.forEach((key, values) -> {
            flattenedHeaders.put(key, String.join(",", values));
        });

        try {
            String contactIds = this.getStringFromList(ids);

            logger.info("Request body as a string ============>: {}", ids);

            ContactAddressWrapper results = contactsDataClient.getContactsByIds(contactIds, flattenedHeaders);

            return results;

        } catch (Exception e) {
            logger.error("Error occurred while fetching administrative areas: {}",
                    e.getMessage());
            ContactAddressWrapper results = new ContactAddressWrapper();
            return results;
        }

    }

    private List<UserDto> getUpdatedByData(Set<Long> ids,
            MultiValueMap<String, String> headers) {
        logger.info("Get update by IDs, size is============>: {}", ids.size());

        headers.add("Content-Type", "application/json");

        Map<String, String> flattenedHeaders = new HashMap<>();
        headers.forEach((key, values) -> {
            flattenedHeaders.put(key, String.join(",", values));
        });

        try {
            String stringIds = this.getStringFromList(ids);

            logger.info("Request body as a string ============>: {}", stringIds);

            List<UserDto> results = usersDataClient.getUsersByIds(stringIds, flattenedHeaders);

            return results;

        } catch (Exception e) {
            logger.error("Error occurred while fetching users: {}",
                    e.getMessage());
            List<UserDto> results = new ArrayList<>();
            return results;
        }

    }

    private String getStringFromList(Set<Long> ids) {
        return ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public List<ProposalDto> getProposalByContact(HttpServletRequest request, Long id) throws Exception {
        // List<Proposal> proposals = proposalRepository.findAll();
        Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());

        List<Proposal> proposalPage = proposalRepository.findByContactIdAndCompanyId(id, orgId);

        List<ProposalDto> proposals = proposalPage.stream().map(proposal -> {
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
            proposalDto.setReferenceNumber(proposal.getReferenceNumber());

            Set<ProposalDocumentDto> proposalDocumentDtos = proposal.getProposalDocuments().stream().map(document -> {
                ProposalDocumentDto documentDto = new ProposalDocumentDto();
                documentDto.setId(document.getId());
                documentDto.setFolderName(document.getFolderName());
                documentDto.setDocumentName(document.getDocumentName());
                documentDto.setBlobUrl(document.getBlobUrl());
                documentDto.setDocumentType(document.getDocumentType());
                documentDto.setUpdatedBy(document.getUpdatedBy());
                documentDto.setCreatedAt(document.getCreatedAt());
                return documentDto;
            }).collect(Collectors.toSet());

            proposalDto.setProposalDocuments(proposalDocumentDtos);

            return proposalDto;
        }).collect(Collectors.toList());

        return proposals;

    }

    public List<ProposalSearchResultDto> searchProposals(HttpServletRequest request,
            MultiValueMap<String, String> headers, String keyword) throws Exception {

        int orgId = (int) jwtService.getTokenValue(jwtService.getJWT(request), "organization-id");
        logger.info("The org id ==============> " + orgId);
        List<Proposal> proposals = proposalRepository.findAll(ProposalSpecification.matchesKeyword(keyword, orgId));

        if (proposals.size() != 0) {
            return proposals.stream().map(
                    proposal -> {
                        ProposalSearchResultDto proposalSearchResultDto = new ProposalSearchResultDto();
                        proposalSearchResultDto.setId(proposal.getId());
                        proposalSearchResultDto.setreferenceNumber(proposal.getReferenceNumber());
                        proposalSearchResultDto.setContactName("");
                        proposalSearchResultDto.setContactIdNumber("");
                        proposalSearchResultDto.setInsuranceCompanyId(proposal.getInsuranceCompanyId());
                        proposalSearchResultDto.setPolicyTypeId(proposal.getPolicyTypeId());
                        proposalSearchResultDto.setContactId(proposal.getContactId());

                        return proposalSearchResultDto;
                    }).collect(Collectors.toList());
        } else {
            headers.add("Content-Type", "application/json");

            Map<String, String> flattenedHeaders = new HashMap<>();
            headers.forEach((key, values) -> {
                flattenedHeaders.put(key, String.join(",", values));
            });

            try {

                List<ContactDto> results = contactsDataClient.getContactsByKeyword(keyword, flattenedHeaders);

                List<Long> contactIds = results.stream()
                        .map(ContactDto::getId)
                        .collect(Collectors.toList());
                logger.info("Got contactIds of size ==============> " + contactIds.size());
                // find proposal by contact ids
                List<ProposalSearchResultDto> proposalResults = proposalRepository
                        .findByContactIdInAndCompanyId(contactIds, orgId)
                        .stream()
                        .map(proposal -> {
                            ProposalSearchResultDto proposalSearchResultDto = new ProposalSearchResultDto();
                            proposalSearchResultDto.setId(proposal.getId());
                            proposalSearchResultDto.setreferenceNumber(proposal.getReferenceNumber());
                            proposalSearchResultDto.setContactName("");
                            proposalSearchResultDto.setContactIdNumber("");
                            proposalSearchResultDto.setInsuranceCompanyId(proposal.getInsuranceCompanyId());
                            proposalSearchResultDto.setPolicyTypeId(proposal.getPolicyTypeId());
                            proposalSearchResultDto.setContactId(proposal.getContactId());

                            return proposalSearchResultDto;
                        }).collect(Collectors.toList());

                return proposalResults;

            } catch (Exception e) {
                logger.error("Error occurred while fetching users: {}",
                        e.getMessage());
                List<ProposalSearchResultDto> results = new ArrayList<>();
                return results;
            }
        }

    }

}
