package com.juxa.legal_advice.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDataDTO {
    private String id;
    private String userId;
    private String name;
    private String email;
    private String phone;
    //Campos opcionales para "Rapida", el usuario podrá seleccionar Omitir, sólo en rápida
    private Integer loginCount;
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
    private String createdAt;
    private String updatedAt;

    @Builder.Default
    private Boolean isPais = false;

    public long getAmountInCents() {
        if (this.amount == null) return 0L;
        return(long) (Double.parseDouble(this.amount)*100);
    }

}
