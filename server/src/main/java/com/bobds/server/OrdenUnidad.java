package com.bobds.server;

public class OrdenUnidad {
    private int idOrden;
    private String idUnidad;

    public OrdenUnidad() {}

    public OrdenUnidad(int idOrden, String idUnidad) {
        this.idOrden = idOrden;
        this.idUnidad = idUnidad;
    }

    public int getIdOrden() {
        return idOrden;
    }

    public void setIdOrden(int idOrden) {
        this.idOrden = idOrden;
    }

    public String getIdUnidad() {
        return idUnidad;
    }

    public void setIdUnidad(String idUnidad) {
        this.idUnidad = idUnidad;
    }
}
