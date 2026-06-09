package com.bobds.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UnitInfoDTO {
    @JsonProperty("nombre")
    private String name;
    @JsonProperty("idUnidad")
    private String unitId;
    @JsonProperty("descripcion")
    private String description;
    @JsonProperty("propietario")
    private String owner;
    @JsonProperty("codVinculacion")
    private String linkCode;
    @JsonProperty("estadoCodigo")
    private String codeStatus;

    public UnitInfoDTO(String name, String unitId, String description, String owner, String linkCode, String codeStatus) {
        this.name = name;
        this.unitId = unitId;
        this.description = description;
        this.owner = owner;
        this.linkCode = linkCode;
        this.codeStatus = codeStatus;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getLinkCode() { return linkCode; }
    public void setLinkCode(String linkCode) { this.linkCode = linkCode; }

    public String getCodeStatus() { return codeStatus; }
    public void setCodeStatus(String codeStatus) { this.codeStatus = codeStatus; }
}
