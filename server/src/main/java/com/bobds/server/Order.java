package com.bobds.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

public class Order {
    @JsonProperty("idOrden")
    private int orderId;

    @JsonProperty("idUnidad")
    private String unitId;

    @JsonProperty("orden")
    private String command;

    @JsonProperty("estado")
    private String status;

    @JsonProperty("emisor")
    private String issuer;

    @JsonProperty("fechaHora")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private String timestamp;

    @JsonProperty("createdAtMs")
    private long createdAtMs;

    @JsonProperty("notas")
    private String notes;

    public Order() {}

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public long getCreatedAtMs() { return createdAtMs; }
    public void setCreatedAtMs(long createdAtMs) { this.createdAtMs = createdAtMs; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
