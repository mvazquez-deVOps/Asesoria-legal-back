package com.juxa.legal_advice.model;

import jakarta.persistence.Embeddable; // O @Entity si es tabla aparte
import lombok.Data;

@Data //
@Embeddable //
public class Message {
    private String role;
    private String text;
    private String timestamp;
}