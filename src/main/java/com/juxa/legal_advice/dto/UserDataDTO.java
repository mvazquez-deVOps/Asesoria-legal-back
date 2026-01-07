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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }

    public String getProcessStatus() {
        return processStatus;
    }

    public void setProcessStatus(String processStatus) {
        this.processStatus = processStatus;
    }

    public String getHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(String hasChildren) {
        this.hasChildren = hasChildren;
    }

    public String getHasViolence() {
        return hasViolence;
    }

    public void setHasViolence(String hasViolence) {
        this.hasViolence = hasViolence;
    }

    public String getDiagnosisPreference() {
        return diagnosisPreference;
    }

    public void setDiagnosisPreference(String diagnosisPreference) {
        this.diagnosisPreference = diagnosisPreference;
    }

    public Boolean getPaid() {
        return isPaid;
    }

    public void setPaid(Boolean paid) {
        isPaid = paid;
    }

    public String getCreateAdt() {
        return createAdt;
    }

    public void setCreateAdt(String createAdt) {
        this.createAdt = createAdt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
