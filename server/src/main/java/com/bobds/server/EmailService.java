package com.bobds.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

@Service
/* servicio de envio de correos */
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /* envio de token de verificacion */
    @Async
    public void enviarVerificacion(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Verify your account - B.O.B.D.S.");
        message.setText("Welcome to the B.O.B.D.S. operational network.\n\n" +
                "Your 6-digit secure access code is: " + token + "\n\n" +
                "Please enter this code on the platform to enable your user.\n\n" +
                "If you did not request this account, please ignore this message.");
        mailSender.send(message);
    }

    /* envio de token de recuperacion */
    @Async
    public void enviarRecuperacionPassword(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Password Recovery - B.O.B.D.S.");
        message.setText("A password reset has been requested for your B.O.B.D.S. account.\n\n" +
                "Your 6-digit secure verification code is: " + token + "\n\n" +
                "Enter this code on the platform to change your password.\n\n" +
                "If you did not request this change, please ignore this message and your password will remain the same.");
        mailSender.send(message);
    }
}