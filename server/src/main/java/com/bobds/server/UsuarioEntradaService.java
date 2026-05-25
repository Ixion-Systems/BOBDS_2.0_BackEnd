package com.bobds.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class UsuarioEntradaService {

    private final String dataFile;
    private final ObjectMapper objectMapper;

    @Autowired
    private EmailService emailService;

    public UsuarioEntradaService() {
        this.dataFile = "data/usuarios.json";
        this.objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    public String signUp(String nombre, String password, String email) {
        String validationResult = Usuario.validarDatos(nombre, password);
        if (!"OK".equals(validationResult)) {
            return "Error de validación:\n" + validationResult;
        }

        try {
            List<Usuario> usuarios = cargarUsuarios();

            // Verificar nombre duplicado
            for (Usuario usuario : usuarios) {
                if (usuario.getNombre().equals(nombre)) {
                    return "Error: El nombre de usuario '" + nombre + "' ya existe.";
                }
                // Verificar email duplicado
                if (email.equals(usuario.getEmail())) {
                    return "Error: El email '" + email + "' ya está registrado.";
                }
            }

            String token = UUID.randomUUID().toString();
            Usuario nuevoUsuario = new Usuario(nombre, password, email);
            nuevoUsuario.setVerificado(false);
            nuevoUsuario.setTokenVerificacion(token);
            usuarios.add(nuevoUsuario);
            guardarUsuarios(usuarios);
            emailService.enviarVerificacion(email, token);

            return "Usuario '" + nombre + "' registrado. Revisá tu email para verificar la cuenta.";
        } catch (IOException e) {
            return "Error al guardar los datos: " + e.getMessage();
        }
    }

    public String verificarEmail(String token) {
        try {
            List<Usuario> usuarios = cargarUsuarios();
            for (Usuario u : usuarios) {
                if (token.equals(u.getTokenVerificacion())) {
                    u.setVerificado(true);
                    u.setTokenVerificacion(null);
                    guardarUsuarios(usuarios);
                    return "Cuenta verificada exitosamente. Ya podés iniciar sesión.";
                }
            }
            return "Error: Token inválido o expirado.";
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }

    public String login(String email, String password) {
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            return "Error: Email y contraseña son requeridos.";
        }
        try {
            List<Usuario> usuarios = cargarUsuarios();
            for (Usuario usuario : usuarios) {
                boolean match = usuario.getEmail() != null && usuario.getEmail().equals(email);
                if (match && usuario.getPassword().equals(password)) {
                    // Bloquear si no verificó el email
                    if (!usuario.isVerificado()) {
                        return "Error: Debés verificar tu email antes de iniciar sesión.";
                    }
                    return "Inicio de sesión exitoso. Bienvenido, " + usuario.getNombre() + "!";
                }
            }
            return "Error: Email o contraseña incorrectos.";
        } catch (IOException e) {
            return "Error al acceder a los datos: " + e.getMessage();
        }
    }

    public String signUpGoogle(String nombre, String email) {
        try {
            List<Usuario> usuarios = cargarUsuarios();
            for (Usuario u : usuarios) {
                if (email.equals(u.getEmail())) {
                    return "Bienvenido de nuevo, " + u.getNombre() + "!";
                }
            }
            Usuario nuevo = new Usuario(nombre, "GOOGLE_AUTH", email);
            nuevo.setVerificado(true); // Google ya verificó el email
            usuarios.add(nuevo);
            guardarUsuarios(usuarios);
            return "Usuario registrado con Google: " + nombre;
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }

    private List<Usuario> cargarUsuarios() throws IOException {
        File file = new File(dataFile);
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }
        try {
            Usuario[] usuariosArray = objectMapper.readValue(file, Usuario[].class);
            return new ArrayList<>(Arrays.asList(usuariosArray)); // lista mutable
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private void guardarUsuarios(List<Usuario> usuarios) throws IOException {
        File file = new File(dataFile);
        objectMapper.writeValue(file, usuarios);
    }
}
