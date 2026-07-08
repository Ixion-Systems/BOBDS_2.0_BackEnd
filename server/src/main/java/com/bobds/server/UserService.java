package com.bobds.server;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Service
/* servicio logico de usuarios */
public class UserService {

    /* dependencias y rutas locales */
    private final String dataFile;
    private final ObjectMapper objectMapper;
    private final Semaphore wrt = new Semaphore(1);
    private final Semaphore mutex = new Semaphore(1);
    private int readCount = 0;

    public void acquireRead() {
        try {
            mutex.acquire();
            readCount++;
            if (readCount == 1) wrt.acquire();
            mutex.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operacion interrumpida", e);
        }
    }

    public void releaseRead() {
        try {
            mutex.acquire();
            readCount--;
            if (readCount == 0) wrt.release();
            mutex.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void acquireWrite() {
        try {
            wrt.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operacion interrumpida", e);
        }
    }

    public void releaseWrite() {
        wrt.release();
    }

    @Autowired
    private EmailService emailService;

    @Autowired
    private LogService logService;

    /* constructores e inicializacion */
    public UserService() {
        this.dataFile = "../data/usuario.json";
        this.objectMapper = new ObjectMapper().enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        File dataDir = new File("../data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    /* utilidades de seguridad */
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

    /* registro tradicional */
    public String signUp(String name, String password, String email) {
        String validationResult = User.validateData(name, password);
        if (!"OK".equals(validationResult)) {
            return "Validation Error:\n" + validationResult;
        }

        acquireWrite();
        try {
            List<User> users = loadUsers();

            for (User user : users) {
                if (email.equals(user.getEmail())) {
                    return "Error: Email '" + email + "' is already registered.";
                }
            }
            int maxId = 0;
            for (User user : users) {
                if (user.getUserId() > maxId) {
                    maxId = user.getUserId();
                }
            }
            int nextId = maxId + 1;

            String token = String.format("%06d", new SecureRandom().nextInt(999999));
            User newUser = new User(name, hashPassword(password), email);
            newUser.setUserId(nextId);
            newUser.setVerified(false);
            newUser.setVerificationToken(token);
            newUser.setTokenGeneratedAtMs(System.currentTimeMillis());
            newUser.setVerificationAttempts(0);

            users.add(newUser);
            saveUsers(users);
            try {
                emailService.enviarVerificacion(email, token);
            } catch (Exception e) {
                users.remove(newUser);
                saveUsers(users);
                return "Critical error sending email. Details: " + e.getMessage();
            }

            try { logService.registerLog(email, 1, "Usuario registrado exitosamente", "Usuario", String.valueOf(nextId)); } catch (Exception ignore) {}
            return "User '" + name + "' registered. Check your email to verify the account.";
        } catch (IOException e) {
            return "Error saving data: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    /* validacion de correo */
    public String verifyEmail(String email, String token) {
        acquireWrite();
        try {
            List<User> users = loadUsers();
            for (User u : users) {
                if (email.equals(u.getEmail())) {
                    if (u.isVerified()) return "Account already verified.";
                    if (u.getVerificationToken() == null) return "Error: No pending code.";

                    long diff = System.currentTimeMillis() - (u.getTokenGeneratedAtMs() != null ? u.getTokenGeneratedAtMs() : 0);
                    if (diff > 15 * 60 * 1000) {
                        return "Error: Code expired (15 min).";
                    }

                    if (token.equals(u.getVerificationToken())) {
                        u.setVerified(true);
                        u.setVerificationToken(null);
                        u.setVerificationAttempts(0);
                        saveUsers(users);
                        return "Account verified successfully. You can now login.";
                    } else {
                        int attempts = (u.getVerificationAttempts() != null ? u.getVerificationAttempts() : 0) + 1;
                        u.setVerificationAttempts(attempts);
                        if (attempts >= 3) {
                            u.setVerificationToken(null);
                            saveUsers(users);
                            return "Error: Too many failed attempts. Code has been revoked. Request a new one.";
                        }
                        saveUsers(users);
                        return "Error: Incorrect code. Attempts left: " + (3 - attempts);
                    }
                }
            }
            return "Error: User not found.";
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    /* reenvio de codigo */
    public String resendCode(String email) {
        acquireWrite();
        try {
            List<User> users = loadUsers();
            for (User u : users) {
                if (email.equals(u.getEmail())) {
                    if (u.isVerified()) {
                        return "Error: Account is already verified.";
                    }
                    String token = String.format("%06d", new SecureRandom().nextInt(999999));
                    u.setVerificationToken(token);
                    u.setTokenGeneratedAtMs(System.currentTimeMillis());
                    u.setVerificationAttempts(0);
                    saveUsers(users);
                    try {
                        emailService.enviarVerificacion(email, token);
                        return "A new 6-digit code has been sent to your email.";
                    } catch (Exception e) {
                        return "Error sending verification email: " + e.getMessage();
                    }
                }
            }
            return "Error: No user found with that email.";
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    /* inicio de sesion local */
    public String login(String email, String password) {
        if (email == null || email.isEmpty() || password == null || password.isEmpty())
            return "Error: Email and password are required.";

        acquireRead();
        try {
            List<User> users = loadUsers();
            for (User user : users) {
                boolean match = user.getEmail() != null && user.getEmail().equals(email);
                if (match) {
                    String hashedInput = hashPassword(password);
                    if (user.getPassword().equals(hashedInput)) {
                        if (!user.isVerified()) {
                            return "Error: You must verify your email before logging in.";
                        }
                        try { logService.registerLog(email, 4, "Inicio de sesión local", "Usuario", String.valueOf(user.getUserId())); } catch (Exception ignore) {}
                        return "Login successful. Welcome, " + user.getUsername() + "!";
                    }
                }
            }
            return "Error: Incorrect email or password.";
        } catch (IOException e) {
            return "Error accessing data: " + e.getMessage();
        } finally {
            releaseRead();
        }
    }

    /* registro y login por google */
    public String signUpGoogle(String name, String email) {
        acquireWrite();
        try {
            List<User> users = loadUsers();
            for (User u : users) {
                if (email.equals(u.getEmail())) {
                    try { logService.registerLog(email, 4, "Inicio de sesión por Google (existente)", "Usuario", String.valueOf(u.getUserId())); } catch (Exception ignore) {}
                    return "Welcome back, " + u.getUsername() + "!";
                }
            }

            int maxId = 0;
            for (User user : users) {
                if (user.getUserId() > maxId) {
                    maxId = user.getUserId();
                }
            }
            int nextId = maxId + 1;

            User newUser = new User(name, "GOOGLE_AUTH", email);
            newUser.setUserId(nextId);
            newUser.setVerified(true);
            users.add(newUser);
            saveUsers(users);
            saveUsers(users);
            try { logService.registerLog(email, 1, "Registro y logueo por Google (nuevo)", "Usuario", String.valueOf(nextId)); } catch (Exception ignore) {}
            return "User registered with Google: " + name;
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    /* solicitud de recuperacion */
    public String requestPasswordReset(String email) {
        acquireWrite();
        try {
            List<User> users = loadUsers();
            for (User u : users) {
                if (email.equals(u.getEmail())) {
                    String token = String.format("%06d", new SecureRandom().nextInt(999999));
                    u.setVerificationToken(token);
                    u.setTokenGeneratedAtMs(System.currentTimeMillis());
                    u.setVerificationAttempts(0);
                    saveUsers(users);
                    try {
                        emailService.enviarRecuperacionPassword(email, token);
                        return "A 6-digit code has been sent to your email to recover your password.";
                    } catch (Exception e) {
                        return "Error sending recovery email: " + e.getMessage();
                    }
                }
            }
            return "Error: No user found with that email.";
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    /* validacion de token temporal */
    public String verifyResetCode(String email, String token) {
        acquireWrite();
        try {
            List<User> users = loadUsers();
            for (User u : users) {
                if (email.equals(u.getEmail())) {
                    if (u.getVerificationToken() == null) return "Error: No pending code.";

                    long diff = System.currentTimeMillis() - (u.getTokenGeneratedAtMs() != null ? u.getTokenGeneratedAtMs() : 0);
                    if (diff > 15 * 60 * 1000) {
                        return "Error: Code expired (15 min).";
                    }

                    if (token.equals(u.getVerificationToken())) {
                        return "Code verified successfully.";
                    } else {
                        int attempts = (u.getVerificationAttempts() != null ? u.getVerificationAttempts() : 0) + 1;
                        u.setVerificationAttempts(attempts);
                        if (attempts >= 3) {
                            u.setVerificationToken(null);
                            saveUsers(users);
                            return "Error: Too many failed attempts. Code revoked. Request a new one.";
                        }
                        saveUsers(users);
                        return "Error: Invalid code. Attempts left: " + (3 - attempts);
                    }
                }
            }
            return "Error: User not found.";
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    /* cambio definitivo de clave */
    public String changePassword(String email, String token, String newPassword) {
        acquireWrite();
        try {
            List<User> users = loadUsers();
            for (User u : users) {
                if (email.equals(u.getEmail())) {
                    if (u.getVerificationToken() == null || !token.equals(u.getVerificationToken())) {
                        return "Error: Invalid, expired, or consumed token.";
                    }
                    String validationResult = User.validateData(u.getUsername(), newPassword);
                    if (!"OK".equals(validationResult)) {
                        return "Validation Error:\n" + validationResult;
                    }
                    u.setPassword(hashPassword(newPassword));
                    u.setVerificationToken(null);
                    saveUsers(users);
                    return "Tu contraseña ha sido actualizada exitosamente. Ya puedes iniciar sesión.";
                }
            }
            return "Error: No se pudo verificar la identidad para cambiar la contraseña.";
        } catch (IOException e) {
            return "Error accessing data: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    /* getters concurrentes eliminados */

    /* persistencia de datos json */
    private List<User> loadUsers() throws IOException {
        File file = new File(dataFile);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        try {
            User[] usersArray = objectMapper.readValue(file, User[].class);
            return new ArrayList<>(Arrays.asList(usersArray));
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private void saveUsers(List<User> users) throws IOException {
        File file = new File(dataFile);
        objectMapper.writeValue(file, users);
    }

    public List<User> getAllUsers() {
        acquireRead();
        try {
            return loadUsers();
        } catch (IOException e) {
            return new ArrayList<>();
        } finally {
            releaseRead();
        }
    }

    /* obtencion de usuario individual */
    public User getUserById(int userId) {
        acquireRead();
        try {
            List<User> users = loadUsers();
            for (User u : users) {
                if (u.getUserId() == userId) return u;
            }
            return null;
        } catch (IOException e) {
            return null;
        } finally {
            releaseRead();
        }
    }

    public User getUserByEmail(String email) {
        if (email == null) return null;
        acquireRead();
        try {
            List<User> users = loadUsers();
            for (User u : users) {
                if (email.equalsIgnoreCase(u.getEmail())) return u;
            }
            return null;
        } catch (IOException e) {
            return null;
        } finally {
            releaseRead();
        }
    }

    public String deleteUser(int userId, String adminEmail) {
        acquireWrite();
        try {
            List<User> users = loadUsers();
            boolean removed = users.removeIf(u -> u.getUserId() == userId);
            if (removed) {
                saveUsers(users);
                // Ideally also delete user's units and orders here if needed, or cascading
                try { logService.registerLog(adminEmail, 5, "Usuario eliminado por admin: " + userId, "Usuario", String.valueOf(userId)); } catch (Exception ignore) {}
                return "OK";
            }
            return "Error: User not found.";
        } catch (IOException e) {
            return "Internal error deleting user: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    public String updateUsername(String email, String newUsername) {
        if (newUsername == null || !newUsername.matches("^[a-zA-Z0-9]{3,30}$")) {
            return "Error: Invalid username format.";
        }
        acquireWrite();
        try {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restaura el estado de interrupción
            }
            List<User> users = loadUsers();
            for (User u : users) {
                if (u.getEmail().equalsIgnoreCase(email)) {
                    u.setUsername(newUsername);
                    saveUsers(users);
                    try { logService.registerLog(email, 3, "Nombre de usuario actualizado", "Usuario", String.valueOf(u.getUserId())); } catch (Exception ignore) {}
                    return "OK";
                }
            }
            return "Error: User not found.";
        } catch (IOException e) {
            return "Internal error updating username: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    public String updatePassword(String email, String currentPassword, String newPassword) {
        acquireWrite();
        try {
            List<User> users = loadUsers();
            User target = null;
            for (User u : users) {
                if (u.getEmail().equalsIgnoreCase(email)) {
                    target = u;
                    break;
                }
            }
            if (target == null) return "Error: User not found.";

            if (!target.getPassword().equals(hashPassword(currentPassword))) {
                return "Error: Current password is incorrect.";
            }

            if (newPassword == null || !newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z0-9]{8,12}$")) {
                return "Error: Password must be 8-12 characters, including upper, lower, and numeric characters.";
            }

            target.setPassword(hashPassword(newPassword));
            saveUsers(users);
            try { logService.registerLog(email, 3, "Contraseña actualizada", "Usuario", String.valueOf(target.getUserId())); } catch (Exception ignore) {}
            return "OK";
        } catch (IOException e) {
            return "Internal error updating password: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    public String updateAnimationPreference(String email, boolean enabled) {
        acquireWrite();
        try {
            List<User> users = loadUsers();
            for (User u : users) {
                if (u.getEmail().equalsIgnoreCase(email)) {
                    u.setAnimationsEnabled(enabled);
                    saveUsers(users);
                    return "OK";
                }
            }
            return "Error: User not found.";
        } catch (IOException e) {
            return "Internal error updating preference: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }
}
