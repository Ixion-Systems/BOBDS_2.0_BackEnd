package com.bobds.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Unidad {
    @JsonProperty("Nombre")
    private String nombre;

    @JsonProperty("IDUnidad")
    private String idUnidad;

    @JsonProperty("Descripcion")
    private String descripcion;

    @JsonProperty("Estado")
    private String estado;

    @JsonProperty("CodVinculacion")
    private String codVinculacion;

    @JsonProperty("CodGeneradoEnMs")
    private Long codGeneradoEnMs;

    @JsonProperty("CodUsado")
    private boolean codUsado;

    public Unidad() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getIdUnidad() { return idUnidad; }
    public void setIdUnidad(String idUnidad) { this.idUnidad = idUnidad; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getCodVinculacion() { return codVinculacion; }
    public void setCodVinculacion(String codVinculacion) { this.codVinculacion = codVinculacion; }

    public Long getCodGeneradoEnMs() { return codGeneradoEnMs; }
    public void setCodGeneradoEnMs(Long codGeneradoEnMs) { this.codGeneradoEnMs = codGeneradoEnMs; }

    public boolean isCodUsado() { return codUsado; }
    public void setCodUsado(boolean codUsado) { this.codUsado = codUsado; }
}
