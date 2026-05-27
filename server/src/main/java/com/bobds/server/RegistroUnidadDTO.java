package com.bobds.server;

public class RegistroUnidadDTO {
    private String nombre;
    private String idUnidad;
    private String descripcion;
    private String codVinculacion;
    private String email;
    private String rol;

    public RegistroUnidadDTO() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getIdUnidad() { return idUnidad; }
    public void setIdUnidad(String idUnidad) { this.idUnidad = idUnidad; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getCodVinculacion() { return codVinculacion; }
    public void setCodVinculacion(String codVinculacion) { this.codVinculacion = codVinculacion; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}
