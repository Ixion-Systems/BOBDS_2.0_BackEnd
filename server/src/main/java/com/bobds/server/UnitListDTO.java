package com.bobds.server;

import com.fasterxml.jackson.annotation.JsonProperty;

/* objeto de transferencia para listar unidades */
public class UnitListDTO {
    /* atributos del dto */
    @JsonProperty("nombre")
    private String name;
    @JsonProperty("idUnidad")
    private String unitId;
    @JsonProperty("estado")
    private String status;
    @JsonProperty("rol")
    private String role;
    @JsonProperty("descripcion")
    private String description;

    /* constructores */
    public UnitListDTO(String name, String unitId, String status, String role, String description) {
        this.name = name;
        this.unitId = unitId;
        this.status = status;
        this.role = role;
        this.description = description;
    }

    /* metodos de acceso */
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
