package com.bobds.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Usuario {

    @JsonProperty("IDUsuario")
    private int idUsuario;

    @JsonProperty("NombreUsuario")
    private String nombreUsuario;

    @JsonProperty("Contraseña")
    private String contraseña;

    @JsonProperty("Email")
    private String email;

    @JsonProperty("Verificado")
    private boolean verificado = false;

    @JsonProperty("TokenVerificacion")
    private String tokenVerificacion;

    public Usuario() {
    }

    public Usuario(String nombreUsuario, String contraseña, String email) {
        this.nombreUsuario = nombreUsuario;
        this.contraseña = contraseña;
        this.email = email;
    }

    /**
     * @param user username to validate
     * @param pass password to validate
     * @return "OK" if both are valid, otherwise a string with error messages
     */
    public static String validarDatos(String user, String pass) {
        StringBuilder errores = new StringBuilder();
        // Validar Usuario: 3-30 caracteres, alfanumérico
        if (user == null || !user.matches("^[a-zA-Z0-9]{3,30}$")) {
            errores.append("- El usuario debe tener entre 3 y 30 caracteres alfanuméricos.\n");
        }

        // Validar Contraseña: 3-12 caracteres, al menos una mayúscula, minúscula y número
        if (pass == null || !pass.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z0-9]{3,12}$")) {
            errores.append("- La contraseña debe tener entre 3 y 12 caracteres(incluyendo mayúscula, minúscula y número).");
        }

        // Si no hubo errores, devolvemos OK
        if (errores.length() == 0) {
            return "OK";
        } else {
            // Si hubo errores, devolvemos el string acumulado
            return errores.toString();
        }
    }

    // Getters y Setters
    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getContraseña() {
        return contraseña;
    }

    public void setContraseña(String contraseña) {
        this.contraseña = contraseña;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isVerificado() {
        return verificado;
    }

    public void setVerificado(boolean verificado) {
        this.verificado = verificado;
    }

    public String getTokenVerificacion() {
        return tokenVerificacion;
    }

    public void setTokenVerificacion(String token) {
        this.tokenVerificacion = token;
    }

    @Override
    public String toString() {
        return "Usuario{"
                + "idUsuario=" + idUsuario
                + ", nombreUsuario='" + nombreUsuario + '\''
                + ", contraseña='[PROTECTED]'"
                + ", email='" + email + '\''
                + ", verificado=" + verificado
                + ", tokenVerificacion='" + tokenVerificacion + '\''
                + '}';
    }
}
