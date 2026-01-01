package com.juxa.legal_advice.dto;

import lombok.Data;

@Data
public class UserDataDTO {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String category;
    private String subcategory;
    private String description;
    private String amount;
    private String location;
    private String counterparty;
    private String processStatus;
    private String hasChildren;
    private String hasViolence;
    private String diagnosisPreference;
    private Boolean isPaid;
    private String createAdt;
    private String userId;



}
