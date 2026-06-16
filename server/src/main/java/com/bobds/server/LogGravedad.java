package com.bobds.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LogGravedad {
    @JsonProperty("idLog")
    private int idLog;

    @JsonProperty("idGravedad")
    private int idGravedad;

    public int getIdLog() { return idLog; }
    public void setIdLog(int idLog) { this.idLog = idLog; }

    public int getIdGravedad() { return idGravedad; }
    public void setIdGravedad(int idGravedad) { this.idGravedad = idGravedad; }
}
