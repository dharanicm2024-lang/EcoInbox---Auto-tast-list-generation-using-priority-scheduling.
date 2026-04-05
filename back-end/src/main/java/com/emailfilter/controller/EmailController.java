package com.emailfilter.controller;

import com.emailfilter.dto.EmailDTO;
import com.emailfilter.dto.ManualEmailRequest;
import com.emailfilter.dto.SendEmailRequest;
import com.emailfilter.service.EmailService;
import com.emailfilter.service.SmtpEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;
    private final SmtpEmailService smtpEmailService;

    @GetMapping
    public ResponseEntity<List<EmailDTO>> getAllEmails() {
        return ResponseEntity.ok(emailService.getAllEmails());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailDTO> getEmailById(@PathVariable Long id) {
        return ResponseEntity.ok(emailService.getEmailById(id));
    }

    @PostMapping
    public ResponseEntity<EmailDTO> submitEmail(@Valid @RequestBody ManualEmailRequest request) {
        EmailDTO result = emailService.processEmail(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/process")
    public ResponseEntity<String> processUnprocessedEmails() {
        emailService.processUnprocessedEmails();
        return ResponseEntity.ok("Processing complete");
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendEmail(@Valid @RequestBody SendEmailRequest request) {
        String result = smtpEmailService.sendEmail(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", result
        ));
    }
}
