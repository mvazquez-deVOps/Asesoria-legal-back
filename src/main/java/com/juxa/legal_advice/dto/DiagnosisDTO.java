package com.juxa.legal_advice.dto;

import lombok.Data;
import java.util.List;

@Data
public class DiagnosisDTO {
    private String id;
    private UserDataDTO userData;
    private List<MessageDTO> chatHistory;
    private String status;
    private String folio;
    private String createdAt; // <-- agregado como String para el mapper actual
}
