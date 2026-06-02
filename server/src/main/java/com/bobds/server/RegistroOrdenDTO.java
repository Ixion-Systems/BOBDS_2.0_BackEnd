package com.bobds.server;

public class RegistroOrdenDTO {
    private String idUnidad;
    private String orden;
    private String notas;

    public RegistroOrdenDTO() {}

    public RegistroOrdenDTO(String idUnidad, String orden, String notas) {
        this.idUnidad = idUnidad;
        this.orden = orden;
        this.notas = notas;
    }

    public String getIdUnidad() {
        return idUnidad;
    }

    public void setIdUnidad(String idUnidad) {
        this.idUnidad = idUnidad;
    }

    public String getOrden() {
        return orden;
    }

    public void setOrden(String orden) {
        this.orden = orden;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }
}
