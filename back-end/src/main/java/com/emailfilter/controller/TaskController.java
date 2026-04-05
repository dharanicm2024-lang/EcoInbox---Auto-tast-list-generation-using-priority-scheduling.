package com.emailfilter.controller;

import com.emailfilter.dto.DashboardStats;
import com.emailfilter.dto.TaskDTO;
import com.emailfilter.entity.Priority;
import com.emailfilter.entity.TaskStatus;
import com.emailfilter.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<TaskDTO>> getAllTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {

        if (status != null) {
            return ResponseEntity.ok(taskService.getTasksByStatus(TaskStatus.valueOf(status.toUpperCase())));
        }
        if (priority != null) {
            return ResponseEntity.ok(taskService.getTasksByPriority(Priority.valueOf(priority.toUpperCase())));
        }
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskDTO> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        TaskStatus status = TaskStatus.valueOf(body.get("status").toUpperCase());
        return ResponseEntity.ok(taskService.updateTaskStatus(id, status));
    }

    @PatchMapping("/{id}/priority")
    public ResponseEntity<TaskDTO> updatePriority(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Priority priority = Priority.valueOf(body.get("priority").toUpperCase());
        return ResponseEntity.ok(taskService.updateTaskPriority(id, priority));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats() {
        return ResponseEntity.ok(taskService.getDashboardStats());
    }
}
