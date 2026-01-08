package com.juxa.legal_advice.dto;

import lombok.Data;
import java.util.List;

@Data
public class DiagnosisDTO {
    private String id;
    private UserDataDTO userData;
    private List<MessageDTO> history; // <--- Cambia 'chatHistory' por 'history'
    private String status;
    private String folio;
    private String createdAt;
}
