package com.bobds.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserUnit {
    @JsonProperty("IDUnidad")
    private String unitId;

    @JsonProperty("Email")
    private String email;

    @JsonProperty("Rol")
    private String role;

    public UserUnit() {}

    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
