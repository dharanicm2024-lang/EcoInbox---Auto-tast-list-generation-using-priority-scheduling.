package com.emailfilter.service;

import com.emailfilter.dto.*;
import com.emailfilter.entity.*;
import com.emailfilter.repository.EmailRepository;
import com.emailfilter.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final EmailRepository emailRepository;
    private final TaskRepository taskRepository;
    private final TaskExtractionService taskExtractionService;
    private final PriorityService priorityService;

    @Transactional
    public EmailDTO processEmail(ManualEmailRequest request) {
        // Create and save the email
        Email email = Email.builder()
                .subject(request.getSubject())
                .sender(request.getSender())
                .body(request.getBody())
                .messageId("manual-" + System.currentTimeMillis())
                .receivedAt(LocalDateTime.now())
                .isProcessed(false)
                .build();

        email = emailRepository.save(email);

        // Extract and save tasks
        processEmailTasks(email);

        return toEmailDTO(email);
    }

    @Transactional
    public void processEmailTasks(Email email) {
        List<ExtractedTask> extractedTasks = taskExtractionService.extractTasks(
                email.getBody(), email.getSubject());

        for (ExtractedTask extracted : extractedTasks) {
            // Deduplication: skip if same task name for same email
            if (taskRepository.existsByTaskNameAndEmail_Id(extracted.getTask(), email.getId())) {
                log.info("Skipping duplicate task: {}", extracted.getTask());
                continue;
            }

            Priority priority = priorityService.fromString(extracted.getPriority());
            // Also check rule-based priority and take the higher one
            Priority rulePriority = priorityService.assignPriority(email.getBody(), email.getSubject());
            if (rulePriority.ordinal() < priority.ordinal()) {
                priority = rulePriority; // HIGH < MEDIUM < LOW in enum order, so lower ordinal = higher priority
            }

            Task task = Task.builder()
                    .email(email)
                    .taskName(extracted.getTask())
                    .deadline(extracted.getDeadline())
                    .priority(priority)
                    .category(extracted.getCategory())
                    .status(TaskStatus.PENDING)
                    .build();

            taskRepository.save(task);
        }

        email.setIsProcessed(true);
        emailRepository.save(email);

        log.info("Processed email '{}' - extracted {} tasks", email.getSubject(), extractedTasks.size());
    }

    @Transactional
    public void processUnprocessedEmails() {
        List<Email> unprocessed = emailRepository.findByIsProcessedFalse();
        log.info("Found {} unprocessed emails", unprocessed.size());
        for (Email email : unprocessed) {
            processEmailTasks(email);
        }
    }

    public List<EmailDTO> getAllEmails() {
        return emailRepository.findAllByOrderByReceivedAtDesc()
                .stream()
                .map(this::toEmailDTO)
                .collect(Collectors.toList());
    }

    public EmailDTO getEmailById(Long id) {
        Email email = emailRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Email not found with id: " + id));
        EmailDTO dto = toEmailDTO(email);
        if (email.getTasks() != null) {
            dto.setTasks(email.getTasks().stream().map(task -> TaskDTO.builder()
                    .id(task.getId())
                    .taskName(task.getTaskName())
                    .deadline(task.getDeadline())
                    .priority(task.getPriority())
                    .status(task.getStatus())
                    .build()).collect(Collectors.toList()));
        }
        return dto;
    }

    private EmailDTO toEmailDTO(Email email) {
        return EmailDTO.builder()
                .id(email.getId())
                .subject(email.getSubject())
                .sender(email.getSender())
                .body(email.getBody())
                .messageId(email.getMessageId())
                .receivedAt(email.getReceivedAt())
                .isProcessed(email.getIsProcessed())
                .taskCount(email.getTasks() != null ? email.getTasks().size() : 0)
                .build();
    }
}
