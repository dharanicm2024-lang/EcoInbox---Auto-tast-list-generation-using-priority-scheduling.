package com.emailfilter.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractedTask {

    private String task;
    private String deadline;
    private String priority;
    private String category;
}
