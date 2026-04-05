package com.emailfilter.service;

import com.emailfilter.entity.Email;
import com.emailfilter.entity.EmailAccount;
import com.emailfilter.repository.EmailAccountRepository;
import com.emailfilter.repository.EmailRepository;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImapFetcherService {

    private final EmailRepository emailRepository;
    private final EmailAccountRepository emailAccountRepository;

    @Value("${email.fetch.max-results:20}")
    private int maxResults;

    /**
     * Fetches new emails from all active email accounts.
     */
    @Transactional
    public List<Email> fetchFromAllAccounts() {
        List<EmailAccount> accounts = emailAccountRepository.findByIsActiveTrue();
        List<Email> allFetched = new ArrayList<>();

        for (EmailAccount account : accounts) {
            try {
                List<Email> fetched = fetchFromAccount(account);
                allFetched.addAll(fetched);
                account.setLastFetchedAt(LocalDateTime.now());
                emailAccountRepository.save(account);
                log.info("Fetched {} new emails from {}", fetched.size(), account.getEmail());
            } catch (Exception e) {
                log.error("Failed to fetch emails from {}: {}", account.getEmail(), e.getMessage());
            }
        }

        return allFetched;
    }

    /**
     * Fetches new emails from a single account via IMAP.
     */
    public List<Email> fetchFromAccount(EmailAccount account) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", account.getImapHost());
        props.put("mail.imaps.port", String.valueOf(account.getImapPort()));
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.ssl.trust", account.getImapHost());
        props.put("mail.imaps.connectiontimeout", "10000");
        props.put("mail.imaps.timeout", "10000");

        Session session = Session.getInstance(props);
        Store store = null;
        Folder inbox = null;
        List<Email> fetchedEmails = new ArrayList<>();

        try {
            store = session.getStore("imaps");
            store.connect(account.getImapHost(), account.getImapPort(),
                    account.getEmail(), account.getAppPassword());

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            int totalMessages = inbox.getMessageCount();
            if (totalMessages == 0) {
                log.info("No messages in inbox for {}", account.getEmail());
                return fetchedEmails;
            }

            // Fetch the latest N messages
            int start = Math.max(1, totalMessages - maxResults + 1);
            Message[] messages = inbox.getMessages(start, totalMessages);

            // Process newest first
            for (int i = messages.length - 1; i >= 0; i--) {
                Message message = messages[i];
                try {
                    String messageId = getMessageId(message);

                    // Skip if already fetched
                    if (emailRepository.existsByMessageId(messageId)) {
                        continue;
                    }

                    Email email = convertToEmail(message, messageId);
                    email = emailRepository.save(email);
                    fetchedEmails.add(email);

                } catch (Exception e) {
                    log.warn("Failed to process message: {}", e.getMessage());
                }
            }

        } finally {
            if (inbox != null && inbox.isOpen()) {
                try { inbox.close(false); } catch (MessagingException ignored) {}
            }
            if (store != null && store.isConnected()) {
                try { store.close(); } catch (MessagingException ignored) {}
            }
        }

        return fetchedEmails;
    }

    /**
     * Tests connection to an email account. Returns a result message.
     */
    public String testConnection(String email, String appPassword, String imapHost, int imapPort) {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", imapHost);
        props.put("mail.imaps.port", String.valueOf(imapPort));
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.ssl.trust", imapHost);
        props.put("mail.imaps.connectiontimeout", "10000");
        props.put("mail.imaps.timeout", "10000");

        try {
            Session session = Session.getInstance(props);
            Store store = session.getStore("imaps");
            store.connect(imapHost, imapPort, email, appPassword);
            int messageCount = store.getFolder("INBOX").getMessageCount();
            store.close();
            log.info("Connection test successful for {} - {} messages in inbox", email, messageCount);
            return "SUCCESS:" + messageCount + " messages in inbox";
        } catch (AuthenticationFailedException e) {
            log.error("Connection test failed for {}: {}", email, e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "Authentication failed";
            if (msg.contains("Application-specific password required") || msg.contains("app password")) {
                return "ERROR:App Password required. Go to myaccount.google.com → Security → 2-Step Verification → App passwords. Generate one and use it instead of your regular password.";
            }
            if (msg.contains("Invalid credentials") || msg.contains("AUTHENTICATIONFAILED")) {
                return "ERROR:Invalid credentials. Make sure you're using an App Password (not your regular password). For Google accounts: myaccount.google.com → Security → App passwords.";
            }
            return "ERROR:Authentication failed: " + msg;
        } catch (Exception e) {
            log.error("Connection test failed for {}: {}", email, e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "Connection failed";
            if (msg.contains("Connection refused") || msg.contains("UnknownHostException")) {
                return "ERROR:Cannot reach " + imapHost + ":" + imapPort + ". Check your IMAP host and port settings.";
            }
            if (msg.contains("timeout") || msg.contains("Timeout")) {
                return "ERROR:Connection timed out. The IMAP server is not responding.";
            }
            return "ERROR:Connection failed: " + msg;
        }
    }

    private Email convertToEmail(Message message, String messageId) throws MessagingException, IOException {
        String subject = message.getSubject() != null ? message.getSubject() : "(No Subject)";
        String sender = extractSender(message);
        String body = extractBody(message);
        LocalDateTime receivedAt = message.getReceivedDate() != null
                ? message.getReceivedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                : LocalDateTime.now();

        // Truncate body if excessively long to avoid DB/AI issues
        if (body.length() > 5000) {
            body = body.substring(0, 5000) + "\n... [truncated]";
        }

        return Email.builder()
                .subject(subject)
                .sender(sender)
                .body(body)
                .messageId(messageId)
                .receivedAt(receivedAt)
                .isProcessed(false)
                .build();
    }

    private String getMessageId(Message message) throws MessagingException {
        String[] headers = message.getHeader("Message-ID");
        if (headers != null && headers.length > 0) {
            return headers[0];
        }
        // Fallback: compose a unique-ish ID from subject + date + sender
        return "imap-" + Math.abs((message.getSubject() + message.getSentDate() + extractSender(message)).hashCode());
    }

    private String extractSender(Message message) throws MessagingException {
        Address[] from = message.getFrom();
        if (from != null && from.length > 0) {
            if (from[0] instanceof InternetAddress ia) {
                return ia.getPersonal() != null
                        ? ia.getPersonal() + " <" + ia.getAddress() + ">"
                        : ia.getAddress();
            }
            return from[0].toString();
        }
        return "Unknown";
    }

    private String extractBody(Message message) throws MessagingException, IOException {
        Object content = message.getContent();

        if (content instanceof String text) {
            return cleanEmailBody(text);
        }

        if (content instanceof MimeMultipart multipart) {
            return extractFromMultipart(multipart);
        }

        return "(Could not extract email body)";
    }

    private String extractFromMultipart(MimeMultipart multipart) throws MessagingException, IOException {
        StringBuilder result = new StringBuilder();
        String plainText = null;

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);

            if (part.isMimeType("text/plain")) {
                plainText = (String) part.getContent();
            } else if (part.isMimeType("text/html") && plainText == null) {
                // Use HTML only if no plain text is available
                String html = (String) part.getContent();
                result.append(stripHtml(html));
            } else if (part.getContent() instanceof MimeMultipart nested) {
                result.append(extractFromMultipart(nested));
            }
        }

        return cleanEmailBody(plainText != null ? plainText : result.toString());
    }

    private String stripHtml(String html) {
        return html
                .replaceAll("<style[^>]*>[\\s\\S]*?</style>", "")
                .replaceAll("<script[^>]*>[\\s\\S]*?</script>", "")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String cleanEmailBody(String body) {
        if (body == null) return "";
        return body
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
