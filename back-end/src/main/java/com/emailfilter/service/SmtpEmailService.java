package com.emailfilter.service;

import com.emailfilter.dto.SendEmailRequest;
import com.emailfilter.entity.EmailAccount;
import com.emailfilter.repository.EmailAccountRepository;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmtpEmailService {

    private final EmailAccountRepository emailAccountRepository;

    public String sendEmail(SendEmailRequest request) {
        EmailAccount account = emailAccountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found: " + request.getAccountId()));

        String smtpHost = account.getSmtpHost();
        int smtpPort = account.getSmtpPort() != null ? account.getSmtpPort() : 587;

        if (smtpHost == null || smtpHost.isBlank()) {
            throw new RuntimeException("SMTP host not configured for account: " + account.getEmail());
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(account.getEmail(), account.getAppPassword());
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(account.getEmail(),
                    account.getDisplayName() != null ? account.getDisplayName() : account.getEmail()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(request.getTo()));
            message.setSubject(request.getSubject());
            message.setText(request.getBody(), "utf-8");

            Transport.send(message);
            log.info("Email sent successfully from {} to {}", account.getEmail(), request.getTo());
            return "Email sent successfully to " + request.getTo();

        } catch (AuthenticationFailedException e) {
            log.error("SMTP auth failed for {}: {}", account.getEmail(), e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "Authentication failed";
            if (msg.contains("Application-specific password") || msg.contains("app password")) {
                throw new RuntimeException("SMTP authentication failed. Use an App Password instead of your regular password.");
            }
            throw new RuntimeException("SMTP authentication failed: " + msg);
        } catch (Exception e) {
            log.error("Failed to send email from {}: {}", account.getEmail(), e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }
}
