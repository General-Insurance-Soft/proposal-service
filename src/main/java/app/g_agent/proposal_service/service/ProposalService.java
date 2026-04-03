package app.g_agent.proposal_service.service;

import java.net.URI;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import app.g_agent.proposal_service.dto.ProposalSaveResponse;
import app.g_agent.proposal_service.dto.ProposalSearchResultDto;
import app.g_agent.proposal_service.dto.UserDto;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

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

    @Value("${backblaze.s3.endpoint-url}")
    private String ENDPOINT_URL;
    @Value("${backblaze.s3.bucket-name}")
    private String bucketName;

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
    public ProposalSaveResponse createProposalWithFileMeta(HttpServletRequest request, ProposalDto proposalDto)
            throws Exception {
        Proposal proposal = this.prepareProposal(request, proposalDto);

        Long userId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "user-id").toString());

        if (proposalDto.getProposalDocuments() != null) {
            logger.info("proposalDto.getProposalDocuments() not null");
            Set<ProposalDocument> proposalDocuments = this.prepareProposalDocumentObject(proposalDto, proposal, userId);
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
    public ProposalSaveResponse createProposal(HttpServletRequest request, ProposalDto proposalDto,
            List<MultipartFile> files) throws Exception {
        Proposal proposal = this.prepareProposal(request, proposalDto);

        Long userId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "user-id").toString());
        Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());

        List<Map<String, String>> proposalDocs = this.uploadFilesToS3(files, orgId);
        Set<ProposalDocumentDto> proposalDocumentDtos = this.prepareProposalDocuments(proposalDocs, proposal, userId);
        proposalDto.setProposalDocuments(proposalDocumentDtos);

        if (proposalDto.getProposalDocuments() != null) {
            logger.info("proposalDto.getProposalDocuments() not null");
            Set<ProposalDocument> proposalDocuments = this.prepareProposalDocumentObject(proposalDto, proposal, userId);
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

    private Proposal prepareProposal(HttpServletRequest request, ProposalDto proposalDto) throws Exception {
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
        return proposal;
    }

    private Set<ProposalDocument> prepareProposalDocumentObject(ProposalDto proposalDto, Proposal proposal,
            Long userId) {
        Set<ProposalDocument> proposalDocuments = new HashSet<>();
        proposalDto.getProposalDocuments().forEach(documentDto -> {
            logger.debug("Document name: " + documentDto.getDocumentName());
            logger.debug("blob url: " + documentDto.getBlobUrl());

            // Skip empty documents
            if (documentDto.getBlobUrl() == null ||
                    documentDto.getDocumentName() == null ||
                    documentDto.getDocumentType() == null) {
                logger.debug("Skipping empty document DTO");
                return;
            }

            ProposalDocument document = new ProposalDocument();
            document.setFolderName(documentDto.getFolderName());
            document.setDocumentName(documentDto.getDocumentName());
            document.setBlobUrl(documentDto.getBlobUrl());
            //SET VERSION ID
            document.setVersionId(documentDto.getVersionId());
            document.setDocumentType(documentDto.getDocumentType());
            document.setUpdatedBy(Long.valueOf(userId));
            document.setProposal(proposal); // Set the proposal reference
            proposalDocuments.add(document);
        });
        return proposalDocuments;
    }

    private Set<ProposalDocumentDto> prepareProposalDocuments(List<Map<String, String>> proposalDocs, Proposal proposal,
            Long userId) {
        Set<ProposalDocumentDto> proposalDocuments = new HashSet<>();

        for (Map<String, String> fileMeta : (List<Map<String, String>>) proposalDocs) {
            ProposalDocumentDto doc = new ProposalDocumentDto();
            doc.setDocumentName(fileMeta.get("name"));
            logger.debug("Blob URL ======>: " + fileMeta.get("blob_url"));
            doc.setBlobUrl((String) fileMeta.get("blob_url"));
            doc.setDocumentType(Long.valueOf(fileMeta.get("type").hashCode())); // Example mapping,
            logger.debug("version id of proposal ======>: " + fileMeta.get("version_id"));
            doc.setVersionId((String)fileMeta.get("version_id"));
            proposalDocuments.add(doc);
        }
        logger.debug("Alter proposal documents DTO ======>: " + proposalDocuments.toString());
        return proposalDocuments;
    }

    private List<Map<String, String>> uploadFilesToS3(List<MultipartFile> filesList, Long orgId) {

        if (orgId == null) {
            logger.warn("Organization ID is null for uploading proposal and user files.");
            return new ArrayList<>();
        }

        Matcher matcher = Pattern.compile("https://s3\\.([a-z0-9-]+)\\.backblazeb2\\.com")
                .matcher(ENDPOINT_URL.trim());
        String region = matcher.find() ? matcher.group(1) : null;
        if (region == null) {
            logger.error("Can't find a region in the endpoint URL: " + ENDPOINT_URL);
            return new ArrayList<>();
        }

        List<Map<String, String>> filesMetaList = new ArrayList<>();
        try {
            S3Client b2 = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(ProfileCredentialsProvider.create("gisca"))
                    .endpointOverride(new URI(ENDPOINT_URL))
                    .build();

            for (MultipartFile file : filesList) {

                String contentType = file.getContentType() != null
                        ? file.getContentType()
                        : "";

                // Accept files with no content type, but reject empty "blob" files
                if (("blob".equalsIgnoreCase(file.getOriginalFilename())
                        || file.getOriginalFilename().isEmpty())
                        && file.getSize() == 0) {
                    logger.info("Skipping empty blob file upload.");
                    continue;
                }

                if (!contentType.isEmpty()
                        && !contentType.matches("image/.*|application/pdf")) {
                    throw new IllegalArgumentException(
                            "Invalid file type: " + file.getOriginalFilename());
                }

                byte[] bytes = file.getBytes();

                String key;

                String input = file.getOriginalFilename();
                String result = input
                        .replaceAll("name:", "")
                        .replaceAll("-type:[^-]+", "");

                key = orgId + "/proposal-service/" + result;

                PutObjectRequest objRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(contentType)
                        .build();

                PutObjectResponse putObjectResponse = b2.putObject(
                        objRequest,
                        software.amazon.awssdk.core.sync.RequestBody.fromBytes(bytes));

                logger.info("Response from S3 for file {}: {}", file.getOriginalFilename(), putObjectResponse.toString());
                
                String fileUrl = String.format(
                        "%s/%s/%s",
                        ENDPOINT_URL,
                        bucketName,
                        key);

                logger.debug("process proposal files ======>: ");
                Map<String, String> filesMeta = new HashMap<>();

                Pattern fileNamePattern = Pattern.compile("name:([^\\-]+)");
                Pattern fileTypePattern = Pattern.compile("-type:([^\\-]+)");

                Matcher fileName = fileNamePattern.matcher(input);
                Matcher typeVal = fileTypePattern.matcher(input);

                logger.debug("process regex and create metadata ======>: ");
                if (fileName.find() && typeVal.find()) {
                    String name = fileName.group(1);
                    String typeNumber = typeVal.group(1);
                    filesMeta.put("name", name);
                    filesMeta.put("type", typeNumber);
                    filesMeta.put("version_id", (String)putObjectResponse.versionId());
                }

                filesMeta.put("blob_url", fileUrl);

                logger.debug("Add objects to map for processing ======>: ");

                filesMetaList.add(filesMeta);

                logger.debug("objects added to map for processing ======>: ");

            }
        } catch (Exception e) {
            logger.error("Error uploading file to S3: " + e.getMessage());
            throw new RuntimeException(e);
        }
        return filesMetaList;
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

        if (proposalDto.getProposalDocuments() != null) {
            logger.info("proposalDto.getProposalDocuments() not null");
            Set<ProposalDocument> proposalDocuments = new HashSet<>();
            proposalDto.getProposalDocuments().forEach(documentDto -> {
                logger.debug("Document name: " + documentDto.getDocumentName());
                logger.debug("blob url: " + documentDto.getBlobUrl());

                // Skip empty documents
                if (documentDto.getBlobUrl() == null ||
                        documentDto.getDocumentName() == null ||
                        documentDto.getDocumentType() == null) {
                    logger.debug("Skipping empty document DTO");
                    return;
                }

                ProposalDocument document = new ProposalDocument();
                document.setFolderName(documentDto.getFolderName());
                document.setDocumentName(documentDto.getDocumentName());
                document.setBlobUrl(documentDto.getBlobUrl());
                document.setDocumentType(documentDto.getDocumentType());
                document.setUpdatedBy(Long.valueOf(userId));
                document.setProposal(proposal); // Set the proposal reference
                proposalDocuments.add(document);
            });
            proposal.getProposalDocuments().clear();
            proposal.getProposalDocuments().addAll(proposalDocuments);
        }

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
