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

    @JsonProperty("fechaHora")
    private String fechaHora;

    @JsonProperty("createdAtMs")
    private Long createdAtMs;

    @JsonProperty("notas")
    private String notas;

    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }

    public Long getCreatedAtMs() { return createdAtMs; }
    public void setCreatedAtMs(Long createdAtMs) { this.createdAtMs = createdAtMs; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    @JsonProperty("finishedAtMs")
    private Long finishedAtMs;

    @JsonProperty("durationMs")
    private Long durationMs;

    @JsonProperty("userEmail")
    private String userEmail;

    public Long getFinishedAtMs() { return finishedAtMs; }
    public void setFinishedAtMs(Long finishedAtMs) { this.finishedAtMs = finishedAtMs; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
}
