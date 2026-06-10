package com.bobds.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

/* entidad de unidad */
public class Unit {
    /* atributos de la entidad */
    @JsonProperty("IDUnidad")
    private String unitId;

    @JsonProperty("Nombre")
    private String name;

    @JsonProperty("Descripcion")
    private String description;

    @JsonProperty("Estado")
    private String status;

    @JsonProperty("CodVinculacion")
    private String linkCode;

    @JsonProperty("CodGeneradoEnMs")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long codeGeneratedAtMs;

    @JsonProperty("CodUsado")
    private boolean codeUsed = false;

    /* constructores */
    public Unit() {}

    /* metodos de acceso */
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLinkCode() { return linkCode; }
    public void setLinkCode(String linkCode) { this.linkCode = linkCode; }

    public Long getCodeGeneratedAtMs() { return codeGeneratedAtMs; }
    public void setCodeGeneratedAtMs(Long codeGeneratedAtMs) { this.codeGeneratedAtMs = codeGeneratedAtMs; }

    public boolean isCodeUsed() { return codeUsed; }
    public void setCodeUsed(boolean codeUsed) { this.codeUsed = codeUsed; }
}
