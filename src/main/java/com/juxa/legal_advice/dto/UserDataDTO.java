package com.juxa.legal_advice.dto;

import lombok.Data;

@Data
public class UserDataDTO {
    private String id;
    private String userId;
    private String name;
    private String email;
    private Integer loginCount;
    private String phone;
    private String userType;
    private String category;
    private String subcategory;
    private String description;
    private String amount;
    private String location;
    private String counterparty;
    private String processStatus;
    private Boolean hasChildren;
    private Boolean hasViolence;
    private String diagnosisPreference;
    private Boolean isPaid;
    private String createdAt;




    public Boolean getPaid() {
        return isPaid;
    }

    public void setPaid(Boolean paid) {
        isPaid = paid;
    }


}
