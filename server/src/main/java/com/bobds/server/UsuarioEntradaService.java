package com.bobds.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.io.IOException;

@Service
public class UsuarioEntradaService {

    private final String dataFile;
    private final ObjectMapper objectMapper;
    private final Semaphore mutex = new Semaphore(1);

    @Autowired
    private EmailService emailService;

    public UsuarioEntradaService() {
        this.dataFile = "../data/usuario.json";
        this.objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        File dataDir = new File("../data");
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
            mutex.acquire();
            try {
                List<Usuario> usuarios = cargarUsuarios();
                for (Usuario usuario : usuarios) {
                    if (usuario.getNombreUsuario().equals(nombre))
                        return "Error: El nombre de usuario '" + nombre + "' ya existe.";
                    if (email.equals(usuario.getEmail()))
                        return "Error: El email '" + email + "' ya está registrado.";
                }
                int maxId = 0;
                for (Usuario usuario : usuarios) {
                    if (usuario.getIdUsuario() > maxId) maxId = usuario.getIdUsuario();
                }
                int nextId = maxId + 1;
                String token = String.format("%06d", new Random().nextInt(999999));
                Usuario nuevoUsuario = new Usuario(nombre, password, email);
                nuevoUsuario.setIdUsuario(nextId);
                nuevoUsuario.setVerificado(false);
                nuevoUsuario.setTokenVerificacion(token);
                usuarios.add(nuevoUsuario);
                guardarUsuarios(usuarios);
                try {
                    emailService.enviarVerificacion(email, token);
                } catch (Exception e) {
                    usuarios.remove(nuevoUsuario);
                    guardarUsuarios(usuarios);
                    return "Error crítico al enviar el email. Detalle: " + e.getMessage();
                }
                return "Usuario '" + nombre + "' registrado. Revisá tu email para verificar la cuenta.";
            } catch (IOException e) {
                return "Error al guardar los datos: " + e.getMessage();
            } finally {
                mutex.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: operación interrumpida.";
        }
    }

    public String verificarEmail(String email, String token) {
        try {
            mutex.acquire();
            try {
                List<Usuario> usuarios = cargarUsuarios();
                for (Usuario u : usuarios) {
                    if (email.equals(u.getEmail()) && token.equals(u.getTokenVerificacion())) {
                        u.setVerificado(true);
                        u.setTokenVerificacion(null);
                        guardarUsuarios(usuarios);
                        return "Cuenta verificada exitosamente. Ya podés iniciar sesión.";
                    }
                }
                return "Error: Token inválido o expirado.";
            } catch (IOException e) {
                return "Error: " + e.getMessage();
            } finally {
                mutex.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: operación interrumpida.";
        }
    }

    public String reenviarCodigo(String email) {
        try {
            mutex.acquire();
            try {
                List<Usuario> usuarios = cargarUsuarios();
                for (Usuario u : usuarios) {
                    if (email.equals(u.getEmail())) {
                        if (u.isVerificado()) return "Error: Esta cuenta ya está verificada.";
                        String token = String.format("%06d", new Random().nextInt(999999));
                        u.setTokenVerificacion(token);
                        guardarUsuarios(usuarios);
                        try {
                            emailService.enviarVerificacion(email, token);
                            return "Se ha reenviado un nuevo código de 6 dígitos a tu correo.";
                        } catch (Exception e) {
                            return "Error al enviar el email: " + e.getMessage();
                        }
                    }
                }
                return "Error: No se encontró un usuario con ese email.";
            } catch (IOException e) {
                return "Error: " + e.getMessage();
            } finally {
                mutex.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: operación interrumpida.";
        }
    }

    public String login(String email, String password) {
        if (email == null || email.isEmpty() || password == null || password.isEmpty())
            return "Error: Email y contraseña son requeridos.";
        try {
            List<Usuario> usuarios = cargarUsuarios();
            for (Usuario usuario : usuarios) {
                boolean match = usuario.getEmail() != null && usuario.getEmail().equals(email);
                if (match && usuario.getContraseña().equals(password)) {
                    if (!usuario.isVerificado())
                        return "Error: Debés verificar tu email antes de iniciar sesión.";
                    return "Inicio de sesión exitoso. Bienvenido, " + usuario.getNombreUsuario() + "!";
                }
            }
            return "Error: Email o contraseña incorrectos.";
        } catch (IOException e) {
            return "Error al acceder a los datos: " + e.getMessage();
        }
    }

    public String signUpGoogle(String nombre, String email) {
        try {
            mutex.acquire();
            try {
                List<Usuario> usuarios = cargarUsuarios();
                for (Usuario u : usuarios) {
                    if (email.equals(u.getEmail()))
                        return "Bienvenido de nuevo, " + u.getNombreUsuario() + "!";
                }
                int maxId = 0;
                for (Usuario usuario : usuarios) {
                    if (usuario.getIdUsuario() > maxId) maxId = usuario.getIdUsuario();
                }
                Usuario nuevo = new Usuario(nombre, "GOOGLE_AUTH", email);
                nuevo.setIdUsuario(maxId + 1);
                nuevo.setVerificado(true);
                usuarios.add(nuevo);
                guardarUsuarios(usuarios);
                return "Usuario registrado con Google: " + nombre;
            } catch (IOException e) {
                return "Error: " + e.getMessage();
            } finally {
                mutex.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: operación interrumpida.";
        }
    }

    public String solicitarRecuperacion(String email) {
        try {
            mutex.acquire();
            try {
                List<Usuario> usuarios = cargarUsuarios();
                for (Usuario u : usuarios) {
                    if (email.equals(u.getEmail())) {
                        String token = String.format("%06d", new Random().nextInt(999999));
                        u.setTokenVerificacion(token);
                        guardarUsuarios(usuarios);
                        try {
                            emailService.enviarRecuperacionPassword(email, token);
                            return "Se ha enviado un código de 6 dígitos a tu correo.";
                        } catch (Exception e) {
                            return "Error al enviar el email: " + e.getMessage();
                        }
                    }
                }
                return "Error: No se encontró un usuario con ese email.";
            } catch (IOException e) {
                return "Error: " + e.getMessage();
            } finally {
                mutex.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: operación interrumpida.";
        }
    }

    public String verificarCodigoRecuperacion(String email, String token) {
        try {
            List<Usuario> usuarios = cargarUsuarios();
            for (Usuario u : usuarios) {
                if (email.equals(u.getEmail()) && token.equals(u.getTokenVerificacion()))
                    return "Código verificado exitosamente.";
            }
            return "Error: Código inválido o expirado.";
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }

    public String cambiarContrasena(String email, String token, String nuevaContrasena) {
        try {
            mutex.acquire();
            try {
                List<Usuario> usuarios = cargarUsuarios();
                for (Usuario u : usuarios) {
                    if (email.equals(u.getEmail()) && token.equals(u.getTokenVerificacion())) {
                        String validationResult = Usuario.validarDatos(u.getNombreUsuario(), nuevaContrasena);
                        if (!"OK".equals(validationResult))
                            return "Error de validación:\n" + validationResult;
                        u.setContraseña(nuevaContrasena);
                        u.setTokenVerificacion(null);
                        guardarUsuarios(usuarios);
                        return "Tu contraseña ha sido actualizada con éxito. Ya puedes iniciar sesión.";
                    }
                }
                return "Error: No se pudo verificar la identidad para cambiar la contraseña.";
            } catch (IOException e) {
                return "Error al acceder a los datos: " + e.getMessage();
            } finally {
                mutex.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: operación interrumpida.";
        }
    }

    private List<Usuario> cargarUsuarios() throws IOException {
        File file = new File(dataFile);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        try {
            Usuario[] usuariosArray = objectMapper.readValue(file, Usuario[].class);
            return new ArrayList<>(Arrays.asList(usuariosArray));
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private void guardarUsuarios(List<Usuario> usuarios) throws IOException {
        File file = new File(dataFile);
        objectMapper.writeValue(file, usuarios);
    }
}