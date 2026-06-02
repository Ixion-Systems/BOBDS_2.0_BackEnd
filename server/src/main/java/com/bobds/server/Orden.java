package com.bobds.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Orden {
    @JsonProperty("IdOrden")
    private int idOrden;

    @JsonProperty("Estado")
    private String estado;

    // Additional fields if needed
    public Orden() {}

    public Orden(int idOrden, String estado) {
        this.idOrden = idOrden;
        this.estado = estado;
    }

    public int getIdOrden() {
        return idOrden;
    }

    public void setIdOrden(int idOrden) {
        this.idOrden = idOrden;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}