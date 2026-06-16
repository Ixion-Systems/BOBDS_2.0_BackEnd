package com.bobds.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LogUsuario {
    @JsonProperty("idLog")
    private int idLog;

    @JsonProperty("email")
    private String email;

    public int getIdLog() { return idLog; }
    public void setIdLog(int idLog) { this.idLog = idLog; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
