package com.bobds.server;

public class Orden {
    private int idOrden;
    private String orden;
    private String notas;
    private String estado;

    public Orden() {}

    public Orden(int idOrden, String orden, String notas, String estado) {
        this.idOrden = idOrden;
        this.orden = orden;
        this.notas = notas;
        this.estado = estado;
    }

    public int getIdOrden() {
        return idOrden;
    }

    public void setIdOrden(int idOrden) {
        this.idOrden = idOrden;
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
