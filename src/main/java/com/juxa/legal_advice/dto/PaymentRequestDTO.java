package com.juxa.legal_advice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentRequestDTO {
    private UserDataDTO userDataDTO;

    public UserDataDTO getUserData() {
    }
}
