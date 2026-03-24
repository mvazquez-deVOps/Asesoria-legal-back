package com.juxa.legal_advice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageResponseDTO {
    private String planName;
    private Integer queriesUsed;
    private Integer queriesLimit;
    private Integer filesUsed;
    private Integer filesLimit;
    private String aiModel;

    // Banderas de control para el Frontend (UI/UX)
    private boolean canMakeMoreQueries;
    private boolean canUploadMoreFiles;
    private boolean canUploadAudio;
    private boolean canUploadVideo;
    private boolean hasFullHistory;
}