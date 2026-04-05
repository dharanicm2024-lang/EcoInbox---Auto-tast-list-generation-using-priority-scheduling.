package com.emailfilter.controller;

import com.emailfilter.dto.EmailAccountDTO;
import com.emailfilter.dto.EmailAccountRequest;
import com.emailfilter.entity.Email;
import com.emailfilter.service.EmailAccountService;
import com.emailfilter.service.EmailService;
import com.emailfilter.service.ImapFetcherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class EmailAccountController {

    private final EmailAccountService emailAccountService;
    private final ImapFetcherService imapFetcherService;
    private final EmailService emailService;

    @GetMapping
    public ResponseEntity<List<EmailAccountDTO>> getAllAccounts() {
        return ResponseEntity.ok(emailAccountService.getAllAccounts());
    }

    @PostMapping
    public ResponseEntity<EmailAccountDTO> addAccount(@Valid @RequestBody EmailAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(emailAccountService.addAccount(request));
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection(@Valid @RequestBody EmailAccountRequest request) {
        String result = emailAccountService.testConnection(request);
        boolean success = result.startsWith("SUCCESS:");
        String message = result.substring(result.indexOf(':') + 1);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "Connection successful! " + message : message
        ));
    }

    @PostMapping("/fetch")
    public ResponseEntity<Map<String, Object>> fetchNow() {
        List<Email> fetched = imapFetcherService.fetchFromAllAccounts();
        // Process the newly fetched emails
        emailService.processUnprocessedEmails();
        return ResponseEntity.ok(Map.of(
                "fetched", fetched.size(),
                "message", fetched.size() + " new emails fetched and processed"
        ));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<EmailAccountDTO> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(emailAccountService.toggleActive(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        emailAccountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
