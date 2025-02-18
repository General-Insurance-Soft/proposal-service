package app.g_agent.proposal_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.g_agent.proposal_service.commons.Message;
import app.g_agent.proposal_service.dto.ProposalDocumentDto;
import app.g_agent.proposal_service.service.ProposalDocumentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/proposal-document")
@Validated
public class ProposalDocumentController {

    private static final Logger logger = LoggerFactory.getLogger(ProposalDocumentController.class);

    @Autowired
    ProposalDocumentService proposalDocumentService;

    @PostMapping("/create")
    public ResponseEntity<?> createProposalDocument(HttpServletRequest request,
            @Valid @RequestBody ProposalDocumentDto proposalDocumentDto) {
        Message message = new Message();

        try {
            proposalDocumentService.createProposalDocument(request, proposalDocumentDto);
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
        message.setName("Success");
        message.setMessage("Proposal document created successfully");
        return ResponseEntity.ok(message);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateProposalDocument(HttpServletRequest request,
            @RequestBody ProposalDocumentDto proposalDocumentDto,
            @RequestParam Long id) {
        Message message = new Message();

        try {
            proposalDocumentService.updateProposalDocument(request, proposalDocumentDto, id);
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
        message.setName("Success");
        message.setMessage("Proposal document updated successfully");
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteProposalDocument(HttpServletRequest request, @RequestParam Long id) {
        Message message = new Message();

        try {
            proposalDocumentService.deleteProposalDocument(request, id);
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
        message.setName("Success");
        message.setMessage("Proposal document deleted successfully");
        return ResponseEntity.ok(message);
    }

    @GetMapping("/get")
    public ResponseEntity<?> getProposalDocument(HttpServletRequest request, @RequestParam Long id) {
        Message message = new Message();

        try {
            return ResponseEntity.ok(proposalDocumentService.getProposalDocument(request, id));
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
    }

    @GetMapping("/get-proposal-documents")
    public ResponseEntity<?> getProposalDocuments(HttpServletRequest request) {
        Message message = new Message();

        try {
            return ResponseEntity.ok(proposalDocumentService.getProposalDocuments(request));
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
    }
}