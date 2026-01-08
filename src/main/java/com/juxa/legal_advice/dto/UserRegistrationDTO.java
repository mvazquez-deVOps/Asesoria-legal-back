package com.juxa.legal_advice.dto;

import lombok.Data;

@Data
public class UserRegistrationDTO {

    private String name;
    private String email;
    private String password;
    private String phone;
}
