package app.g_agent.proposal_service.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProposalDocumentDto {

    private Long id;

    @JsonProperty("proposal_id")
    private Long proposalId;

    @NotBlank(message = "Folder name is required")
    @JsonProperty("folder_name")
    private String folderName;

    @NotBlank(message = "Document name is required")
    @JsonProperty("document_name")
    private String documentName;

    @NotBlank(message = "Blob URL is required")
    @JsonProperty("blob_url")
    private String blobUrl;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("uploaded_at")
    private LocalDateTime uploadedAt;

    @NotNull(message = "Updated by is required")
    @JsonProperty("updated_by")
    private Long updatedBy;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProposalId() {
        return proposalId;
    }

    public void setProposalId(Long proposalId) {
        this.proposalId = proposalId;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getBlobUrl() {
        return blobUrl;
    }

    public void setBlobUrl(String blobUrl) {
        this.blobUrl = blobUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}