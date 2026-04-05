package com.emailfilter.dto;

import com.emailfilter.entity.Priority;
import com.emailfilter.entity.TaskStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDTO {

    private Long id;
    private Long emailId;
    private String emailSubject;
    private String emailSender;
    private String taskName;
    private String deadline;
    private Priority priority;
    private String category;
    private TaskStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
