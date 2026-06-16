package com.bobds.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

/* entidad de usuario */
public class User {

    /* atributos de la entidad */
    @JsonProperty("IDUsuario")
    private int userId;

    @JsonProperty("NombreUsuario")
    private String username;

    @JsonProperty("Contraseña")
    private String password;

    @JsonProperty("Email")
    private String email;

    @JsonProperty("Verificado")
    private boolean verified = false;

    @JsonProperty("TokenVerificacion")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String verificationToken;

    @JsonProperty("TokenGeneradoEnMs")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long tokenGeneratedAtMs;

    @JsonProperty("IntentosVerificacion")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer verificationAttempts = 0;

    @JsonProperty("isAdmin")
    private boolean isAdmin = false;

    /* constructores */
    public User() {
    }

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    /* logica de validacion */
    public static String validateData(String user, String pass) {
        StringBuilder errors = new StringBuilder();
        if (user == null || !user.matches("^[a-zA-Z0-9]{3,30}$")) {
            errors.append("- Username must be 3-30 alphanumeric characters.\n");
        }

        if (pass == null || !pass.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z0-9]{3,12}$")) {
            errors.append("- Password must be 3-12 characters, including upper, lower, and numeric characters.");
        }

        if (errors.length() == 0) {
            return "OK";
        } else {
            return errors.toString();
        }
    }

    /* metodos de acceso */
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String token) { this.verificationToken = token; }

    public Long getTokenGeneratedAtMs() { return tokenGeneratedAtMs; }
    public void setTokenGeneratedAtMs(Long tokenGeneratedAtMs) { this.tokenGeneratedAtMs = tokenGeneratedAtMs; }

    public Integer getVerificationAttempts() { return verificationAttempts; }
    public void setVerificationAttempts(Integer verificationAttempts) { this.verificationAttempts = verificationAttempts; }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }
}
