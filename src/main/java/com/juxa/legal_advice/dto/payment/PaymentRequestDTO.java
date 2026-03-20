package com.juxa.legal_advice.dto.payment;

import com.juxa.legal_advice.dto.UserDataDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentRequestDTO {
    private UserDataDTO userDataDTO;

    public UserDataDTO getUserData() {
        return this.userDataDTO; //
    }
}
