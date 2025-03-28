package app.g_agent.proposal_service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import app.g_agent.proposal_service.model.ProposalDocument;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProposalDto {

	private Long id;

	@NotNull(message = "insurance_company_id is required")
	@JsonProperty("insurance_company_id")
	private Long insuranceCompanyId;

	@NotNull(message = "policy_type_id is required")
	@JsonProperty("policy_type_id")
	private Long policyTypeId;

	@JsonProperty("start_date")
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate startDate;

	@JsonProperty("end_date")
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate endDate;

	@JsonProperty("created_at")
	private LocalDateTime createdAt;

	@JsonProperty("updated_at")
	private LocalDateTime updatedAt;

	@JsonProperty("updated_by")
	private Long updatedBy;

	@NotNull(message = "Company ID is required")
	@JsonProperty("company_id")
	private Long companyId;

	@NotNull(message = "Contact ID is required")
	@JsonProperty("contact_id")
	private Long contactId;

	@JsonProperty("proposal_documents")
	private Set<ProposalDocumentDto> proposalDocuments;

	@JsonProperty("reference_number")
	@NotBlank(message = "reference_number is required")
	@NotNull(message = "reference_number is required")
	private String referenceNumber;

	public void setReferenceNumber(String referenceNumber) {
		this.referenceNumber = referenceNumber;
	}

	public String getReferenceNumber() {
		return referenceNumber;
	}

	// Getters and Setters

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getInsuranceCompanyId() {
		return insuranceCompanyId;
	}

	public void setInsuranceCompanyId(Long insuranceCompanyId) {
		this.insuranceCompanyId = insuranceCompanyId;
	}

	public Long getPolicyTypeId() {
		return policyTypeId;
	}

	public void setPolicyTypeId(Long policyTypeId) {
		this.policyTypeId = policyTypeId;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Long getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(Long updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Long getCompanyId() {
		return companyId;
	}

	public void setCompanyId(Long companyId) {
		this.companyId = companyId;
	}

	public Long getContactId() {
		return contactId;
	}

	public void setContactId(Long contactId) {
		this.contactId = contactId;
	}

	public Set<ProposalDocumentDto> getProposalDocuments() {
		return proposalDocuments;
	}

	public void setProposalDocuments(Set<ProposalDocumentDto> proposalDocuments) {
		this.proposalDocuments = proposalDocuments;
	}
}