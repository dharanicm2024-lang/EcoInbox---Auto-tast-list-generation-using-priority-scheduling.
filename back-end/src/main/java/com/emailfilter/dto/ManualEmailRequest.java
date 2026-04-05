package com.emailfilter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ManualEmailRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Sender is required")
    private String sender;

    @NotBlank(message = "Body is required")
    private String body;
}
