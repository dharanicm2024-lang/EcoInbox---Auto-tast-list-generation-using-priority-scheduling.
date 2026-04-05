package com.emailfilter.service;

import com.emailfilter.dto.ExtractedTask;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
@Slf4j
public class OpenAIService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    private static final String SYSTEM_PROMPT = """
            You are an assistant that extracts actionable tasks from emails with high accuracy.
            
            Focus on identifying these specific event types and categories:
            - Events: Hackathon, Competition, Contest, Workshop, Seminar, Webinar, Conference, Symposium, Bootcamp, Training, Internship, Placement Drive, Recruitment, Scholarship, Fellowship, Program, Meetup.
            - Academic: Assignment, Project submission, Exam, Internal test, Viva, Lab, Record submission.
            
            Rules:
            - Only include actionable items (e.g., Register, Apply, Submit, Upload, Attend, Join, Pay fee).
            - Extract deadlines specifically mentioned (e.g., "Last date", "Apply before", "Closing date").
            - Assign priority:
                - HIGH: Urgent, Mandatory, Compulsory, Immediate, Deadline today/tomorrow.
                - MEDIUM: Important, Recommended, Suggested, Project submissions.
                - LOW: Optional, If interested, FYI.
            - If a task belongs to a specific category (e.g., Hackathon, Internship), include it in the 'category' field.
            - Ignore greetings, signatures, and disclaimers.
            
            Return ONLY valid JSON array:
            [
              {
                "task": "string - concise task description",
                "deadline": "string - deadline if mentioned, or null",
                "priority": "LOW | MEDIUM | HIGH",
                "category": "string - e.g., Hackathon, Internship, Assignment, or null"
              }
            ]
            """;

    public OpenAIService(@Value("${openai.base-url}") String baseUrl, ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = objectMapper;
    }

    public List<ExtractedTask> extractTasks(String emailContent) {
        try {
            Map<String, Object> request = buildRequest(emailContent);

            String response = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseResponse(response);

        } catch (Exception e) {
            log.error("OpenAI API call failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Map<String, Object> buildRequest(String emailContent) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", model);
        request.put("temperature", 0.3);
        request.put("max_tokens", 1000);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", "Email:\n\"\"\"\n" + emailContent + "\n\"\"\""));
        request.put("messages", messages);

        return request;
    }

    @SuppressWarnings("unchecked")
    private List<ExtractedTask> parseResponse(String response) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(response, new TypeReference<>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices == null || choices.isEmpty()) return Collections.emptyList();

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            // Clean the response - remove markdown code blocks if present
            content = content.trim();
            if (content.startsWith("```")) {
                content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
            }

            return objectMapper.readValue(content, new TypeReference<List<ExtractedTask>>() {});

        } catch (Exception e) {
            log.error("Failed to parse OpenAI response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
