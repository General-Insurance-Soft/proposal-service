package app.g_agent.proposal_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

public class ProposalSearchResultDto {
    private Long id;

    @JsonProperty("reference_number")
    private String referenceNumber;

    @JsonProperty("contact_name")
    private String contactName;

    @JsonProperty("contact_id_number")
    private String contactIdNumber;

    @JsonProperty("contact_id")
    private Long contactId;

    @NotNull(message = "insurance_company_id is required")
    @JsonProperty("insurance_company_id")
    private Long insuranceCompanyId;

    @NotNull(message = "policy_type_id is required")
    @JsonProperty("policy_type_id")
    private Long policyTypeId;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getreferenceNumber() {
        return referenceNumber;
    }

    public void setreferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactIdNumber() {
        return contactIdNumber;
    }

    public void setContactIdNumber(String contactIdNumber) {
        this.contactIdNumber = contactIdNumber;
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

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

}
