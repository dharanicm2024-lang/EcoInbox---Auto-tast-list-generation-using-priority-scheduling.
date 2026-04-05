package com.emailfilter.service;

import com.emailfilter.dto.DashboardStats;
import com.emailfilter.dto.TaskDTO;
import com.emailfilter.entity.Priority;
import com.emailfilter.entity.Task;
import com.emailfilter.entity.TaskStatus;
import com.emailfilter.repository.EmailRepository;
import com.emailfilter.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final EmailRepository emailRepository;

    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAllOrderByPriorityAndDate()
                .stream()
                .map(this::toTaskDTO)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status)
                .stream()
                .map(this::toTaskDTO)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> getTasksByPriority(Priority priority) {
        return taskRepository.findByPriority(priority)
                .stream()
                .map(this::toTaskDTO)
                .collect(Collectors.toList());
    }

    public TaskDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
        return toTaskDTO(task);
    }

    @Transactional
    public TaskDTO updateTaskStatus(Long id, TaskStatus status) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
        task.setStatus(status);
        task = taskRepository.save(task);
        log.info("Updated task {} status to {}", id, status);
        return toTaskDTO(task);
    }

    @Transactional
    public TaskDTO updateTaskPriority(Long id, Priority priority) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
        task.setPriority(priority);
        task = taskRepository.save(task);
        log.info("Updated task {} priority to {}", id, priority);
        return toTaskDTO(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
        log.info("Deleted task {}", id);
    }

    public DashboardStats getDashboardStats() {
        return DashboardStats.builder()
                .totalTasks(taskRepository.count())
                .pendingTasks(taskRepository.countByStatus(TaskStatus.PENDING))
                .completedTasks(taskRepository.countByStatus(TaskStatus.DONE))
                .highPriorityTasks(taskRepository.countByPriority(Priority.HIGH))
                .mediumPriorityTasks(taskRepository.countByPriority(Priority.MEDIUM))
                .lowPriorityTasks(taskRepository.countByPriority(Priority.LOW))
                .totalEmails(emailRepository.count())
                .processedEmails(emailRepository.count() - emailRepository.findByIsProcessedFalse().size())
                .build();
    }

    private TaskDTO toTaskDTO(Task task) {
        return TaskDTO.builder()
                .id(task.getId())
                .emailId(task.getEmail() != null ? task.getEmail().getId() : null)
                .emailSubject(task.getEmail() != null ? task.getEmail().getSubject() : null)
                .emailSender(task.getEmail() != null ? task.getEmail().getSender() : null)
                .taskName(task.getTaskName())
                .deadline(task.getDeadline())
                .priority(task.getPriority())
                .category(task.getCategory())
                .status(task.getStatus())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
