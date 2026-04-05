package com.emailfilter.service;

import com.emailfilter.entity.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailSchedulerService {

    private final EmailService emailService;
    private final ImapFetcherService imapFetcherService;

    @Scheduled(cron = "${email.fetch.cron}")
    public void fetchAndProcessEmails() {
        log.info("Scheduled email fetch & processing started...");

        // Step 1: Fetch new emails from all connected accounts
        try {
            List<Email> fetched = imapFetcherService.fetchFromAllAccounts();
            log.info("Fetched {} new emails from connected accounts", fetched.size());
        } catch (Exception e) {
            log.error("Error fetching emails: {}", e.getMessage());
        }

        // Step 2: Process all unprocessed emails (including manually submitted)
        emailService.processUnprocessedEmails();
        log.info("Scheduled email fetch & processing completed.");
    }
}
