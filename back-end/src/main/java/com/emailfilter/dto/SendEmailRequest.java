package com.emailfilter.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailRequest {

    @NotNull(message = "Sender account ID is required")
    private Long accountId;

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Must be a valid recipient email")
    private String to;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Body is required")
    private String body;
}
