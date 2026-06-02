package com.bobds.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    // Nota: La contraseña de envío (spring.mail.password) se inyecta automáticamente 
    // a través del archivo ignorado 'application-secret.properties' por seguridad.
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void enviarVerificacion(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Verificá tu cuenta - B.O.B.D.S.");
        message.setText("Bienvenido a la red operativa de B.O.B.D.S.\n\n" +
                "Tu código de acceso seguro de 6 dígitos es: " + token + "\n\n" +
                "Por favor, ingresá este código en la plataforma para habilitar tu usuario.\n\n" +
                "Si no solicitaste esta cuenta, ignora este mensaje.");
        mailSender.send(message);
    }

    public void enviarRecuperacionPassword(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Recuperación de Contraseña - B.O.B.D.S.");
        message.setText("Se ha solicitado un restablecimiento de contraseña para tu cuenta en B.O.B.D.S.\n\n" +
                "Tu código seguro de verificación de 6 dígitos es: " + token + "\n\n" +
                "Ingresá este código en la plataforma para cambiar tu contraseña.\n\n" +
                "Si no solicitaste este cambio, por favor ignora este mensaje y tu contraseña seguirá siendo la misma.");
        mailSender.send(message);
    }
}