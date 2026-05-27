package com.bobds.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UsuarioUnidad {
    @JsonProperty("IDUnidad")
    private String idUnidad;

    @JsonProperty("Email")
    private String email;

    @JsonProperty("Rol")
    private String rol;

    public UsuarioUnidad() {}

    public String getIdUnidad() { return idUnidad; }
    public void setIdUnidad(String idUnidad) { this.idUnidad = idUnidad; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}
