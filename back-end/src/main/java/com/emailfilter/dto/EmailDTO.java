package com.emailfilter.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailDTO {

    private Long id;
    private String subject;
    private String sender;
    private String body;
    private String messageId;
    private LocalDateTime receivedAt;
    private Boolean isProcessed;
    private int taskCount;
    private List<TaskDTO> tasks;
}
