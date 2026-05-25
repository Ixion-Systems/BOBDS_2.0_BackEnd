package com.bobds.server;
  import com.fasterxml.jackson.annotation.JsonProperty;

  public class Usuario {

      @JsonProperty("nombre")
      private String nombre;

      @JsonProperty("password")
      private String password;

      @JsonProperty("email")
      private String email;

      public Usuario() {
      }

      public Usuario(String nombre, String password) {
          this.nombre = nombre;
          this.password = password;
      }

      public Usuario(String nombre, String password, String email) {
          this.nombre = nombre;
          this.password = password;
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
      public String getNombre() {
          return nombre;
      }

      public void setNombre(String nombre) {
          this.nombre = nombre;
      }

      public String getPassword() {
          return password;
      }

      public void setPassword(String password) {
          this.password = password;
      }

      public String getEmail() {
          return email;
      }

      public void setEmail(String email) {
          this.email = email;
      }

      @Override
      public String toString() {
          return "Usuario{"
                  + "nombre='" + nombre + '\''
                  + ", password='[PROTECTED]'"
                  + ", email='" + email + '\''
                  + '}';
      }
  }