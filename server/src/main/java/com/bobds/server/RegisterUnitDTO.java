package com.bobds.server;

public class RegisterUnitDTO {
    private String nombre;
    private String descripcion;
    private String userEmail;
    private String idUnidad;
    private String codVinculacion;

    public RegisterUnitDTO() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getIdUnidad() { return idUnidad; }
    public void setIdUnidad(String idUnidad) { this.idUnidad = idUnidad; }

    public String getCodVinculacion() { return codVinculacion; }
    public void setCodVinculacion(String codVinculacion) { this.codVinculacion = codVinculacion; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
}
