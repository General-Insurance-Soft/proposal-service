package app.g_agent.proposal_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProposalSearchResultDto {
    private Long id;

    @JsonProperty("reference_number")
    private String referenceNumber;

    @JsonProperty("contact_name")
    private String contactName;

    @JsonProperty("contact_id_number")
    private String contactIdNumber;

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

}
