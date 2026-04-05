package com.emailfilter.service;

import com.emailfilter.dto.ExtractedTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskExtractionService {

    private final OpenAIService openAIService;
    private final RuleBasedExtractionService ruleBasedService;
    private final PriorityService priorityService;

    @Value("${openai.api-key}")
    private String apiKey;

    /**
     * Extracts tasks from email content.
     * Uses AI if OpenAI key is configured, otherwise falls back to rule-based extraction.
     */
    public List<ExtractedTask> extractTasks(String emailBody, String subject) {
        if (isAIEnabled()) {
            log.info("Using AI-based task extraction");
            List<ExtractedTask> tasks = openAIService.extractTasks(emailBody);
            if (!tasks.isEmpty()) {
                return tasks;
            }
            log.warn("AI extraction returned empty, falling back to rule-based");
        }

        log.info("Using rule-based task extraction");
        return ruleBasedService.extractTasks(emailBody, subject);
    }

    private boolean isAIEnabled() {
        return apiKey != null
                && !apiKey.isBlank()
                && !apiKey.equals("your-openai-api-key-here");
    }
}
