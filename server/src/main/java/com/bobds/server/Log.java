package com.bobds.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

public class Log {
    @JsonProperty("idLog")
    private int idLog;

    @JsonProperty("descripcion")
    private String descripcion;

    @JsonProperty("fechaHora")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private String fechaHora;

    @JsonProperty("tipoEntidad")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String tipoEntidad;

    @JsonProperty("entidadId")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String entidadId;

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
}
