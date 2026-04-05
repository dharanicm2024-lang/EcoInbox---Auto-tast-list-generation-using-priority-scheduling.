package com.emailfilter.service;

import com.emailfilter.dto.ExtractedTask;
import com.emailfilter.entity.Priority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class RuleBasedExtractionService {

    private static final List<Pattern> ACTION_PATTERNS = List.of(
            Pattern.compile("(?i)please\\s+(.{5,80})"),
            Pattern.compile("(?i)(?:can you|could you|would you)\\s+(.{5,80})"),
            Pattern.compile("(?i)(?:need to|need you to)\\s+(.{5,80})"),
            Pattern.compile("(?i)(?:submit|schedule|complete|review|prepare|send|update|create|finalize|deliver|organize|arrange|register|apply|upload|fill form|attend|join|participate|confirm|pay fee|download|present)\\s+(.{3,80})"),
            Pattern.compile("(?i)(?:make sure|ensure|don't forget)\\s+(?:to\\s+)?(.{5,80})"),
            Pattern.compile("(?i)(?:deadline|due|by|last date|submission date|apply before|register before|closing date|final date|ends on|expires on|within \\d+ days)\\s+(?:is\\s+)?(.{3,50})")
    );

    private static final Pattern DEADLINE_INDICATOR_PATTERN = Pattern.compile(
            "(?i)(?:by|before|deadline|due|until|last date|submission date|apply before|register before|closing date|final date|ends on|expires on)"
    );

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?i)(tomorrow|today|tonight|this week|monday|tuesday|wednesday|thursday|friday|saturday|sunday|end of (?:day|week|month)|\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?|within \\d+ days)"
    );

    private static final List<String> EVENT_KEYWORDS = List.of(
            "Hackathon", "Competition", "Contest", "Workshop", "Seminar", "Webinar", "Conference", 
            "Symposium", "Bootcamp", "Training", "Internship", "Placement Drive", "Recruitment", 
            "Scholarship", "Fellowship", "Program", "Meetup", "Assignment", "Project submission", 
            "Exam", "Internal test", "Viva", "Lab", "Record submission"
    );

    public List<ExtractedTask> extractTasks(String emailBody, String subject) {
        List<ExtractedTask> tasks = new ArrayList<>();
        String content = emailBody + " " + subject;

        // Split into sentences
        String[] sentences = content.split("[.!?\\n]+");

        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.length() < 5) continue;

            for (Pattern pattern : ACTION_PATTERNS) {
                Matcher matcher = pattern.matcher(sentence);
                if (matcher.find()) {
                    String taskText = cleanTaskText(matcher.group(0));
                    if (taskText.length() >= 5 && taskText.length() <= 150) {
                        String deadline = extractDeadline(sentence);
                        String priority = determinePriority(sentence);
                        String category = determineCategory(sentence);

                        tasks.add(ExtractedTask.builder()
                                .task(taskText)
                                .deadline(deadline)
                                .priority(priority)
                                .category(category)
                                .build());
                        break; // One task per sentence
                    }
                }
            }
        }

        return tasks;
    }

    private String extractDeadline(String sentence) {
        Matcher indicatorMatcher = DEADLINE_INDICATOR_PATTERN.matcher(sentence);
        if (indicatorMatcher.find()) {
            // Find the rest of the sentence after the indicator
            String afterIndicator = sentence.substring(indicatorMatcher.end());
            Matcher dateMatcher = DATE_PATTERN.matcher(afterIndicator);
            if (dateMatcher.find()) {
                return dateMatcher.group(1).trim();
            }
            // Try matching in the whole sentence as a fallback
            Matcher fullDateMatcher = DATE_PATTERN.matcher(sentence);
            if (fullDateMatcher.find()) {
                return fullDateMatcher.group(1).trim();
            }
        }
        return null;
    }

    private String determinePriority(String sentence) {
        String lower = sentence.toLowerCase();
        if (lower.contains("urgent") || lower.contains("asap") || lower.contains("immediately") ||
                lower.contains("critical") || lower.contains("emergency") || lower.contains("mandatory") ||
                lower.contains("compulsory") || lower.contains("important") || lower.contains("immediate")) {
            return "HIGH";
        }
        if (lower.contains("tomorrow") || lower.contains("today") || lower.contains("eod") ||
                lower.contains("tonight") || lower.contains("deadline") || lower.contains("last date")) {
            return "HIGH";
        }
        if (lower.contains("fyi") || lower.contains("no rush") || lower.contains("when you can") ||
                lower.contains("optional") || lower.contains("if interested")) {
            return "LOW";
        }
        if (lower.contains("recommended") || lower.contains("suggested")) {
            return "MEDIUM";
        }
        return "MEDIUM";
    }

    private String determineCategory(String sentence) {
        String lower = sentence.toLowerCase();
        for (String keyword : EVENT_KEYWORDS) {
            if (lower.contains(keyword.toLowerCase())) {
                return keyword;
            }
        }
        return "Task";
    }

    private String cleanTaskText(String text) {
        return text.replaceAll("(?i)^(please|can you|could you|would you|need to|need you to)\\s+", "")
                   .replaceAll("[\\r\\n]+", " ")
                   .trim();
    }
}
