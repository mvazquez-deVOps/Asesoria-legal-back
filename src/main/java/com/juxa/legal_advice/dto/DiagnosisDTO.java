package com.juxa.legal_advice.dto;

import com.juxa.legal_advice.model.Message;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DiagnosisDTO {
    private String id;
    private String userId;
    private String type;
    private UserDataDTO userData;
    private Map<String, String> answers;   // <---
    private Integer score;
    private String result;
    private List<Message> chatHistory;
}
