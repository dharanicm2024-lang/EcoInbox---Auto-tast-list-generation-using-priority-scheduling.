package com.emailfilter.service;

import com.emailfilter.dto.EmailAccountDTO;
import com.emailfilter.dto.EmailAccountRequest;
import com.emailfilter.entity.EmailAccount;
import com.emailfilter.repository.EmailAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailAccountService {

    private final EmailAccountRepository emailAccountRepository;
    private final ImapFetcherService imapFetcherService;

    // Common IMAP settings for popular providers
    private static final Map<String, String> IMAP_HOSTS = Map.of(
            "gmail.com", "imap.gmail.com",
            "googlemail.com", "imap.gmail.com",
            "outlook.com", "outlook.office365.com",
            "hotmail.com", "outlook.office365.com",
            "live.com", "outlook.office365.com",
            "yahoo.com", "imap.mail.yahoo.com",
            "icloud.com", "imap.mail.me.com",
            "me.com", "imap.mail.me.com",
            "zoho.com", "imap.zoho.com"
    );

    // Common SMTP settings for popular providers
    private static final Map<String, String> SMTP_HOSTS = Map.of(
            "gmail.com", "smtp.gmail.com",
            "googlemail.com", "smtp.gmail.com",
            "outlook.com", "smtp-mail.outlook.com",
            "hotmail.com", "smtp-mail.outlook.com",
            "live.com", "smtp-mail.outlook.com",
            "yahoo.com", "smtp.mail.yahoo.com",
            "icloud.com", "smtp.mail.me.com",
            "me.com", "smtp.mail.me.com",
            "zoho.com", "smtp.zoho.com"
    );

    // Google Workspace uses the same IMAP/SMTP servers as Gmail
    // Domains with MX records pointing to Google (e.g., edu.in, org domains)
    private static final java.util.Set<String> GOOGLE_WORKSPACE_INDICATORS = java.util.Set.of(
            "edu.in", "edu", "ac.in", "org", "org.in"
    );

    private String resolveImapHost(String email) {
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        String host = IMAP_HOSTS.get(domain);
        if (host != null) return host;

        // Check if it might be a Google Workspace domain (educational/org)
        for (String suffix : GOOGLE_WORKSPACE_INDICATORS) {
            if (domain.endsWith("." + suffix)) {
                return "imap.gmail.com";
            }
        }
        return "imap." + domain;
    }

    private String resolveSmtpHost(String email) {
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        String host = SMTP_HOSTS.get(domain);
        if (host != null) return host;

        for (String suffix : GOOGLE_WORKSPACE_INDICATORS) {
            if (domain.endsWith("." + suffix)) {
                return "smtp.gmail.com";
            }
        }
        return "smtp." + domain;
    }

    @Transactional
    public EmailAccountDTO addAccount(EmailAccountRequest request) {
        if (emailAccountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email account already exists: " + request.getEmail());
        }

        String imapHost = request.getImapHost();
        int imapPort = request.getImapPort() != null ? request.getImapPort() : 993;
        String smtpHost = request.getSmtpHost();
        int smtpPort = request.getSmtpPort() != null ? request.getSmtpPort() : 587;

        // Auto-detect IMAP host from email domain
        if (imapHost == null || imapHost.isBlank()) {
            imapHost = resolveImapHost(request.getEmail());
        }
        // Auto-detect SMTP host from email domain
        if (smtpHost == null || smtpHost.isBlank()) {
            smtpHost = resolveSmtpHost(request.getEmail());
        }

        EmailAccount account = EmailAccount.builder()
                .email(request.getEmail())
                .appPassword(request.getAppPassword())
                .imapHost(imapHost)
                .imapPort(imapPort)
                .smtpHost(smtpHost)
                .smtpPort(smtpPort)
                .displayName(request.getDisplayName() != null ? request.getDisplayName() : request.getEmail())
                .isActive(true)
                .build();

        account = emailAccountRepository.save(account);
        log.info("Added email account: {}", account.getEmail());
        return toDTO(account);
    }

    public String testConnection(EmailAccountRequest request) {
        String imapHost = request.getImapHost();
        int imapPort = request.getImapPort() != null ? request.getImapPort() : 993;

        if (imapHost == null || imapHost.isBlank()) {
            imapHost = resolveImapHost(request.getEmail());
        }

        return imapFetcherService.testConnection(
                request.getEmail(), request.getAppPassword(), imapHost, imapPort);
    }

    public List<EmailAccountDTO> getAllAccounts() {
        return emailAccountRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public EmailAccountDTO toggleActive(Long id) {
        EmailAccount account = emailAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found: " + id));
        account.setIsActive(!account.getIsActive());
        account = emailAccountRepository.save(account);
        log.info("Toggled account {} active: {}", account.getEmail(), account.getIsActive());
        return toDTO(account);
    }

    @Transactional
    public void deleteAccount(Long id) {
        emailAccountRepository.deleteById(id);
        log.info("Deleted email account: {}", id);
    }

    private EmailAccountDTO toDTO(EmailAccount account) {
        return EmailAccountDTO.builder()
                .id(account.getId())
                .email(account.getEmail())
                .imapHost(account.getImapHost())
                .imapPort(account.getImapPort())
                .smtpHost(account.getSmtpHost())
                .smtpPort(account.getSmtpPort())
                .displayName(account.getDisplayName())
                .isActive(account.getIsActive())
                .lastFetchedAt(account.getLastFetchedAt())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
