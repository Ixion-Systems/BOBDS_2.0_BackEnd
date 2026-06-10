package com.bobds.server;

import com.fasterxml.jackson.annotation.JsonProperty;

/* entidad relacion orden-unidad */
public class OrderUnit {
    /* atributos de la entidad */
    @JsonProperty("idOrden")
    private int orderId;

    @JsonProperty("idUnidad")
    private String unitId;

    /* constructores */
    public OrderUnit() {}

    /* metodos de acceso */
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
}
