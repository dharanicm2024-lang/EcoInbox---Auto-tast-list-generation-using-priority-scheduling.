package com.emailfilter.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailAccountDTO {

    private Long id;
    private String email;
    private String imapHost;
    private Integer imapPort;
    private String smtpHost;
    private Integer smtpPort;
    private String displayName;
    private Boolean isActive;
    private LocalDateTime lastFetchedAt;
    private LocalDateTime createdAt;
}
