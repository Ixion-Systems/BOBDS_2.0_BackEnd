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
import java.security.SecureRandom;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class UserService {

    private final String dataFile;
    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Autowired
    private EmailService emailService;

    public UserService() {
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

    public String signUp(String name, String password, String email) {
        String validationResult = User.validateData(name, password);
        if (!"OK".equals(validationResult)) {
            return "Validation Error:\n" + validationResult;
        }

        lock.writeLock().lock();
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

            return "User '" + name + "' registered. Check your email to verify the account.";
        } catch (IOException e) {
            return "Error saving data: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String verifyEmail(String email, String token) {
        lock.writeLock().lock();
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
            lock.writeLock().unlock();
        }
    }

    public String resendCode(String email) {
        lock.writeLock().lock();
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
            lock.writeLock().unlock();
        }
    }

    public String login(String email, String password) {
        if (email == null || email.isEmpty() || password == null || password.isEmpty())
            return "Error: Email and password are required.";
        
        lock.readLock().lock();
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
                        return "Login successful. Welcome, " + user.getUsername() + "!";
                    }
                }
            }
            return "Error: Incorrect email or password.";
        } catch (IOException e) {
            return "Error accessing data: " + e.getMessage();
        } finally {
            lock.readLock().unlock();
        }
    }

    public String signUpGoogle(String name, String email) {
        lock.writeLock().lock();
        try {
            List<User> users = loadUsers();
            for (User u : users) {
                if (email.equals(u.getEmail()))
                    return "Welcome back, " + u.getUsername() + "!";
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
            return "User registered with Google: " + name;
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String requestPasswordReset(String email) {
        lock.writeLock().lock();
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
            lock.writeLock().unlock();
        }
    }

    public String verifyResetCode(String email, String token) {
        lock.writeLock().lock();
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
            lock.writeLock().unlock();
        }
    }

    public String changePassword(String email, String token, String newPassword) {
        lock.writeLock().lock();
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
            lock.writeLock().unlock();
        }
    }

    public ReentrantReadWriteLock getLock() {
        return lock;
    }

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
}
