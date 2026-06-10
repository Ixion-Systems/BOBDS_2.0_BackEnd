package com.bobds.server;

import com.fasterxml.jackson.annotation.JsonProperty;

/* objeto de transferencia para registrar orden */
public class RegisterOrderDTO {
    /* atributos del dto */
    @JsonProperty("idUnidad")
    private String unitId;
    @JsonProperty("orden")
    private String command;
    private String issuerEmail;
    @JsonProperty("notas")
    private String notes;

    /* constructores */
    public RegisterOrderDTO() {}

    /* metodos de acceso */
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getIssuerEmail() { return issuerEmail; }
    public void setIssuerEmail(String issuerEmail) { this.issuerEmail = issuerEmail; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
