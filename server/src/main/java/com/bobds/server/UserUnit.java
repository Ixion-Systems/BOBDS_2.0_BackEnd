package com.bobds.server;

import com.fasterxml.jackson.annotation.JsonProperty;

/* entidad relacion usuario-unidad */
public class UserUnit {
    /* atributos de la entidad */
    @JsonProperty("IDUnidad")
    private String unitId;

    @JsonProperty("Email")
    private String email;

    @JsonProperty("Rol")
    private String role;

    @JsonProperty("VinculadoEnMs")
    private Long vinculadoEnMs;

    /* constructores */
    public UserUnit() {}

    /* metodos de acceso */
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getVinculadoEnMs() { return vinculadoEnMs; }
    public void setVinculadoEnMs(Long vinculadoEnMs) { this.vinculadoEnMs = vinculadoEnMs; }
}
