package com.bobds.server;

public class UnidadListadoDTO {
    private String nombre;
    private String idUnidad;
    private String estado;
    private String rol;

    public UnidadListadoDTO(String nombre, String idUnidad, String estado, String rol) {
        this.nombre = nombre;
        this.idUnidad = idUnidad;
        this.estado = estado;
        this.rol = rol;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getIdUnidad() { return idUnidad; }
    public void setIdUnidad(String idUnidad) { this.idUnidad = idUnidad; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}
