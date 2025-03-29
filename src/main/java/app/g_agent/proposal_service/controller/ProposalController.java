package app.g_agent.proposal_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import app.g_agent.proposal_service.dto.ProposalDto;
import app.g_agent.proposal_service.dto.ProposalSaveResponse;
import app.g_agent.proposal_service.service.ProposalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/proposal")
@Validated
public class ProposalController {

    private static final Logger logger = LoggerFactory.getLogger(ProposalController.class);

    @Autowired
    ProposalService proposalService;

    @PostMapping("/create")
    public ResponseEntity<?> createProposal(HttpServletRequest request, @Valid @RequestBody ProposalDto proposalDto) {
        Message message = new Message();

        try {

            return new ResponseEntity<ProposalSaveResponse>(proposalService.createProposal(request, proposalDto),
                    HttpStatus.CREATED);
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }

    }

    @PutMapping("/update")
    public ResponseEntity<?> updateProposal(HttpServletRequest request, @RequestBody ProposalDto proposalDto,
            @RequestParam Long id) {
        Message message = new Message();

        try {
            proposalService.updateProposal(request, proposalDto, id);
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
        message.setName("Success");
        message.setMessage("Proposal updated successfully");
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteProposal(HttpServletRequest request, @RequestParam Long id) {
        Message message = new Message();

        try {
            proposalService.deleteProposal(request, id);
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
        message.setName("Success");
        message.setMessage("Proposal deleted successfully");
        return ResponseEntity.ok(message);
    }

    @GetMapping("/get")
    public ResponseEntity<?> getProposal(HttpServletRequest request, @RequestParam Long id) {
        Message message = new Message();

        try {
            return ResponseEntity.ok(proposalService.getProposal(request, id));
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
    }

    @GetMapping("/get-proposals")
    public ResponseEntity<?> getProposals(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Message message = new Message();

        try {
            return ResponseEntity.ok(proposalService.getProposals(request, page, size));
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
    }
}