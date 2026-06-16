package com.bobds.server;

import com.fasterxml.jackson.annotation.JsonProperty;

/* entidad de orden */
public class Order {
    /* atributos de la entidad */
    @JsonProperty("idOrden")
    private int orderId;

    @JsonProperty("orden")
    private String command;

    @JsonProperty("estado")
    private String status;

    /* constructores */
    public Order() {}

    /* metodos de acceso */
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
