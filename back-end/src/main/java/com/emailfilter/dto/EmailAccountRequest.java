package com.emailfilter.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailAccountRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "App password is required")
    private String appPassword;

    private String imapHost;
    private Integer imapPort;
    private String smtpHost;
    private Integer smtpPort;
    private String displayName;
}
