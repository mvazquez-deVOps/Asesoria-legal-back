package com.juxa.legal_advice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormatDTO {
    private String id;
    private String title;
    private String description;
    private String fileUrl;
    private int downloads;
    private String category;
}
