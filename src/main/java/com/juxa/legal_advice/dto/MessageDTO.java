package com.juxa.legal_advice.dto;

import lombok.Data;

import java.util.List;
@Data
public class MessageDTO {
    private String role;
    private String text;
    private Boolean isError;
    private List<String> suggestions;
    private String timestamp;
}
