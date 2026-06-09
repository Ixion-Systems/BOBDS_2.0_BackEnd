package com.bobds.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderUnit {
    @JsonProperty("idOrden")
    private int orderId;

    @JsonProperty("idUnidad")
    private String unitId;

    public OrderUnit() {}

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
}
