package com.bobds.server;

  import com.fasterxml.jackson.databind.ObjectMapper;  import com.fasterxml.jackson.databind.SerializationFeature;
  import org.springframework.stereotype.Service;
  import java.io.File;
  import java.io.IOException;
  import java.util.ArrayList;
  import java.util.List;

  @Service
  public class UsuarioEntradaService {

      private final String dataFile;
      private final ObjectMapper objectMapper;

      public UsuarioEntradaService() {
          this.dataFile = "data/usuarios.json";
          this.objectMapper = new ObjectMapper();

          // Configure ObjectMapper for pretty printing
          objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
          // Ensure data directory exists
          File dataDir = new File("data");
          if (!dataDir.exists()) {
              dataDir.mkdirs();
          }
      }

      /**
       * @param nombre
       * @param password 
       * @param email
       * @return
       */
      public String signUp(String nombre, String password, String email) {
          String validationResult = Usuario.validarDatos(nombre, password);
          if (!"OK".equals(validationResult)) {
              return "Error de validación:\n" + validationResult;
          }

          try {
              Usuario nuevoUsuario = new Usuario(nombre, password, email);
              List<Usuario> usuarios = cargarUsuarios();
              for (Usuario usuario : usuarios) {
                  if (usuario.getNombre().equals(nombre)) {
                      return "Error: El nombre de usuario '" + nombre + "' ya existe.";
                  }
              }
              usuarios.add(nuevoUsuario);
              guardarUsuarios(usuarios);
              return "Usuario '" + nombre + "' registrado exitosamente.";
          } catch (IOException e) {
              return "Error al guardar los datos: " + e.getMessage();
          }
      }

      /**
       * @param email
       * @param password 
       * @return 
       */
      public String login(String email, String password) {
          if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            return "Error: Email y contraseña son requeridos.";
          }
          try {
              List<Usuario> usuarios = cargarUsuarios();
              for (Usuario usuario : usuarios) {
                  boolean match = usuario.getEmail() != null && usuario.getEmail().equals(email);
                  if (match && usuario.getPassword().equals(password)) {
                      return "Inicio de sesión exitoso. Bienvenido, " + usuario.getNombre() + "!";
                  }
              }
              return "Error: Email o contraseña incorrectos.";
          } catch (IOException e) {
              return "Error al acceder a los datos: " + e.getMessage();
          }
      }

      /**
       * @return list of users (empty list if file doesn't exist or is empty)
       * @throws IOException if there's an error reading the file
       */
      private List<Usuario> cargarUsuarios() throws IOException {
          File file = new File(dataFile);
          if (!file.exists()) {
              return new ArrayList<>();
          }
          if (file.length() == 0) {
              return new ArrayList<>();
          }
          try {
              Usuario[] usuariosArray = objectMapper.readValue(file,Usuario[].class);
              return java.util.Arrays.asList(usuariosArray);
          } catch (IOException e) {
              return new ArrayList<>();
          }
      }

      /**
       * @param usuarios
       * @throws IOException
       */
      private void guardarUsuarios(List<Usuario> usuarios) throws IOException {
          File file = new File(dataFile);
          objectMapper.writeValue(file, usuarios);
      }
  }