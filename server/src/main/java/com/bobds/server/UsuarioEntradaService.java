package com.bobds.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class UsuarioEntradaService {

    private final String dataFile;
    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

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

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    public String signUp(String nombre, String password, String email) {
        String validationResult = Usuario.validarDatos(nombre, password);
        if (!"OK".equals(validationResult)) {
            return "Error de validación:\n" + validationResult;
        }

        lock.writeLock().lock();
        try {
            List<Usuario> usuarios = cargarUsuarios();

            // Verificar email duplicado
            for (Usuario usuario : usuarios) {
                if (email.equals(usuario.getEmail())) {
                    return "Error: El email '" + email + "' ya está registrado.";
                }
            }
            int maxId = 0;
            for (Usuario usuario : usuarios) {
                if (usuario.getIdUsuario() > maxId) {
                    maxId = usuario.getIdUsuario();
                }
            }
            int nextId = maxId + 1;

            String token = String.format("%06d", new Random().nextInt(999999));
            Usuario nuevoUsuario = new Usuario(nombre, hashPassword(password), email);
            nuevoUsuario.setIdUsuario(nextId);
            nuevoUsuario.setVerificado(false);
            nuevoUsuario.setTokenVerificacion(token);
            nuevoUsuario.setTokenGeneradoEnMs(System.currentTimeMillis());
            nuevoUsuario.setIntentosVerificacion(0);
            
            usuarios.add(nuevoUsuario);
            guardarUsuarios(usuarios);
            try {
                emailService.enviarVerificacion(email, token);
            } catch (Exception e) {
                usuarios.remove(nuevoUsuario);
                guardarUsuarios(usuarios);
                return "Error crítico al enviar el email de verificación. Revisa la configuración del servidor o prueba de nuevo. Detalle: " + e.getMessage();
            }

            return "Usuario '" + nombre + "' registrado. Revisá tu email para verificar la cuenta.";
        } catch (IOException e) {
            return "Error al guardar los datos: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String verificarEmail(String email, String token) {
        lock.writeLock().lock();
        try {
            List<Usuario> usuarios = cargarUsuarios();
            for (Usuario u : usuarios) {
                if (email.equals(u.getEmail())) {
                    if (u.isVerificado()) return "Cuenta ya verificada.";
                    if (u.getTokenVerificacion() == null) return "Error: No hay un código pendiente.";
                    
                    long diff = System.currentTimeMillis() - (u.getTokenGeneradoEnMs() != null ? u.getTokenGeneradoEnMs() : 0);
                    if (diff > 15 * 60 * 1000) {
                        return "Error: El código ha expirado por tiempo (15 min).";
                    }

                    if (token.equals(u.getTokenVerificacion())) {
                        u.setVerificado(true);
                        u.setTokenVerificacion(null);
                        u.setIntentosVerificacion(0);
                        guardarUsuarios(usuarios);
                        return "Cuenta verificada exitosamente. Ya podés iniciar sesión.";
                    } else {
                        int intentos = (u.getIntentosVerificacion() != null ? u.getIntentosVerificacion() : 0) + 1;
                        u.setIntentosVerificacion(intentos);
                        if (intentos >= 3) {
                            u.setTokenVerificacion(null);
                            guardarUsuarios(usuarios);
                            return "Error: Demasiados intentos fallidos. Por seguridad, el código ha sido anulado. Solicita uno nuevo.";
                        }
                        guardarUsuarios(usuarios);
                        return "Error: Código incorrecto. Intentos restantes: " + (3 - intentos);
                    }
                }
            }
            return "Error: No se encontró el usuario.";
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String reenviarCodigo(String email) {
        lock.writeLock().lock();
        try {
            List<Usuario> usuarios = cargarUsuarios();
            for (Usuario u : usuarios) {
                if (email.equals(u.getEmail())) {
                    if (u.isVerificado()) {
                        return "Error: Esta cuenta ya está verificada.";
                    }
                    String token = String.format("%06d", new Random().nextInt(999999));
                    u.setTokenVerificacion(token);
                    u.setTokenGeneradoEnMs(System.currentTimeMillis());
                    u.setIntentosVerificacion(0);
                    guardarUsuarios(usuarios);
                    try {
                        emailService.enviarVerificacion(email, token);
                        return "Se ha reenviado un nuevo código de 6 dígitos a tu correo.";
                    } catch (Exception e) {
                        return "Error al enviar el email de verificación: " + e.getMessage();
                    }
                }
            }
            return "Error: No se encontró un usuario con ese email.";
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String login(String email, String password) {
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            return "Error: Email y contraseña son requeridos.";
        }
        lock.readLock().lock();
        try {
            List<Usuario> usuarios = cargarUsuarios();
            for (Usuario usuario : usuarios) {
                boolean match = usuario.getEmail() != null && usuario.getEmail().equals(email);
                if (match) {
                    // Soporte legacy: si la contraseña en JSON no está hasheada, permitimos texto plano (útil durante migración)
                    String hashedInput = hashPassword(password);
                    if (usuario.getContraseña().equals(hashedInput) || usuario.getContraseña().equals(password)) {
                        if (!usuario.isVerificado()) {
                            return "Error: Debés verificar tu email antes de iniciar sesión.";
                        }
                        return "Inicio de sesión exitoso. Bienvenido, " + usuario.getNombreUsuario() + "!";
                    }
                }
            }
            return "Error: Email o contraseña incorrectos.";
        } catch (IOException e) {
            return "Error al acceder a los datos: " + e.getMessage();
        } finally {
            lock.readLock().unlock();
        }
    }

    public String signUpGoogle(String nombre, String email) {
        lock.writeLock().lock();
        try {
            List<Usuario> usuarios = cargarUsuarios();
            for (Usuario u : usuarios) {
                if (email.equals(u.getEmail())) {
                    return "Bienvenido de nuevo, " + u.getNombreUsuario() + "!";
                }
            }
            int maxId = 0;
            for (Usuario usuario : usuarios) {
                if (usuario.getIdUsuario() > maxId) {
                    maxId = usuario.getIdUsuario();
                }
            }
            int nextId = maxId + 1;

            Usuario nuevo = new Usuario(nombre, "GOOGLE_AUTH", email);
            nuevo.setIdUsuario(nextId);
            nuevo.setVerificado(true);
            usuarios.add(nuevo);
            guardarUsuarios(usuarios);
            return "Usuario registrado con Google: " + nombre;
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String solicitarRecuperacion(String email) {
        lock.writeLock().lock();
        try {
            List<Usuario> usuarios = cargarUsuarios();
            for (Usuario u : usuarios) {
                if (email.equals(u.getEmail())) {
                    String token = String.format("%06d", new Random().nextInt(999999));
                    u.setTokenVerificacion(token);
                    u.setTokenGeneradoEnMs(System.currentTimeMillis());
                    u.setIntentosVerificacion(0);
                    guardarUsuarios(usuarios);
                    try {
                        emailService.enviarRecuperacionPassword(email, token);
                        return "Se ha enviado un código de 6 dígitos a tu correo para recuperar tu contraseña.";
                    } catch (Exception e) {
                        return "Error al enviar el email de recuperación: " + e.getMessage();
                    }
                }
            }
            return "Error: No se encontró un usuario con ese email.";
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String verificarCodigoRecuperacion(String email, String token) {
        lock.writeLock().lock();
        try {
            List<Usuario> usuarios = cargarUsuarios();
            for (Usuario u : usuarios) {
                if (email.equals(u.getEmail())) {
                    if (u.getTokenVerificacion() == null) return "Error: No hay código pendiente.";
                    
                    long diff = System.currentTimeMillis() - (u.getTokenGeneradoEnMs() != null ? u.getTokenGeneradoEnMs() : 0);
                    if (diff > 15 * 60 * 1000) {
                        return "Error: El código ha expirado por tiempo (15 min).";
                    }

                    if (token.equals(u.getTokenVerificacion())) {
                        return "Código verificado exitosamente.";
                    } else {
                        int intentos = (u.getIntentosVerificacion() != null ? u.getIntentosVerificacion() : 0) + 1;
                        u.setIntentosVerificacion(intentos);
                        if (intentos >= 3) {
                            u.setTokenVerificacion(null);
                            guardarUsuarios(usuarios);
                            return "Error: Demasiados intentos fallidos. Código anulado. Solicita uno nuevo.";
                        }
                        guardarUsuarios(usuarios);
                        return "Error: Código inválido. Intentos restantes: " + (3 - intentos);
                    }
                }
            }
            return "Error: No se encontró el usuario.";
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String cambiarContrasena(String email, String token, String nuevaContrasena) {
        lock.writeLock().lock();
        try {
            List<Usuario> usuarios = cargarUsuarios();
            for (Usuario u : usuarios) {
                if (email.equals(u.getEmail())) {
                    if (u.getTokenVerificacion() == null || !token.equals(u.getTokenVerificacion())) {
                        return "Error: Token inválido, expirado o ya consumido.";
                    }
                    String validationResult = Usuario.validarDatos(u.getNombreUsuario(), nuevaContrasena);
                    if (!"OK".equals(validationResult)) {
                        return "Error de validación:\n" + validationResult;
                    }
                    u.setContraseña(hashPassword(nuevaContrasena));
                    u.setTokenVerificacion(null);
                    guardarUsuarios(usuarios);
                    return "Tu contraseña ha sido actualizada con éxito. Ya puedes iniciar sesión.";
                }
            }
            return "Error: No se pudo verificar la identidad para cambiar la contraseña.";
        } catch (IOException e) {
            return "Error al acceder a los datos: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<Usuario> cargarUsuarios() throws IOException {
        File file = new File(dataFile);
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }
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
