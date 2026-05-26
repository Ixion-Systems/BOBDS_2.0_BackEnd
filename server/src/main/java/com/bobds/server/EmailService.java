package com.bobds.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

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
}