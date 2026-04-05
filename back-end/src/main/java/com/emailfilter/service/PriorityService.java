package com.emailfilter.service;

import com.emailfilter.entity.Email;
import com.emailfilter.entity.Priority;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class PriorityService {

    private static final Set<String> HIGH_PRIORITY_WORDS = Set.of(
            "urgent", "asap", "immediately", "critical", "emergency",
            "deadline", "overdue", "right away", "top priority", "eod",
            "end of day", "by today", "as soon as possible"
    );

    private static final Set<String> HIGH_DEADLINE_WORDS = Set.of(
            "tomorrow", "tonight", "by today", "within 24 hours",
            "by end of day", "eod", "this morning", "this afternoon"
    );

    private static final Set<String> LOW_PRIORITY_INDICATORS = Set.of(
            "fyi", "for your information", "newsletter", "no action needed",
            "just sharing", "no rush", "when you get a chance",
            "low priority", "not urgent", "whenever possible"
    );

    public Priority assignPriority(String emailBody, String subject) {
        String content = (emailBody + " " + subject).toLowerCase();

        // Check for high priority
        for (String word : HIGH_PRIORITY_WORDS) {
            if (content.contains(word)) {
                return Priority.HIGH;
            }
        }

        // Check for tight deadlines
        for (String word : HIGH_DEADLINE_WORDS) {
            if (content.contains(word)) {
                return Priority.HIGH;
            }
        }

        // Check for low priority indicators
        for (String word : LOW_PRIORITY_INDICATORS) {
            if (content.contains(word)) {
                return Priority.LOW;
            }
        }

        return Priority.MEDIUM;
    }

    public Priority fromString(String priority) {
        if (priority == null) return Priority.MEDIUM;
        return switch (priority.toUpperCase().trim()) {
            case "HIGH" -> Priority.HIGH;
            case "LOW" -> Priority.LOW;
            default -> Priority.MEDIUM;
        };
    }
}
