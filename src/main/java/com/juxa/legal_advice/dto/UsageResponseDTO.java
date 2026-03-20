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
    private Integer queriesLimit; // -1 significa ilimitado
    private Integer filesUsed;
    private Integer filesLimit;
    private String aiModel;
}