package com.bobds.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LogDetailDTO {
    @JsonProperty("idLog")
    private int idLog;

    @JsonProperty("descripcion")
    private String descripcion;

    @JsonProperty("fechaHora")
    private String fechaHora;

    @JsonProperty("tipoEntidad")
    private String tipoEntidad;

    @JsonProperty("entidadId")
    private String entidadId;

    @JsonProperty("gravedad")
    private int gravedad;

    @JsonProperty("emailUsuario")
    private String emailUsuario;

    public LogDetailDTO(int idLog, String descripcion, String fechaHora, String tipoEntidad, String entidadId, int gravedad, String emailUsuario) {
        this.idLog = idLog;
        this.descripcion = descripcion;
        this.fechaHora = fechaHora;
        this.tipoEntidad = tipoEntidad;
        this.entidadId = entidadId;
        this.gravedad = gravedad;
        this.emailUsuario = emailUsuario;
    }

    // Getters and setters
    public int getIdLog() { return idLog; }
    public void setIdLog(int idLog) { this.idLog = idLog; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }

    public String getTipoEntidad() { return tipoEntidad; }
    public void setTipoEntidad(String tipoEntidad) { this.tipoEntidad = tipoEntidad; }

    public String getEntidadId() { return entidadId; }
    public void setEntidadId(String entidadId) { this.entidadId = entidadId; }

    public int getGravedad() { return gravedad; }
    public void setGravedad(int gravedad) { this.gravedad = gravedad; }

    public String getEmailUsuario() { return emailUsuario; }
    public void setEmailUsuario(String emailUsuario) { this.emailUsuario = emailUsuario; }
}
