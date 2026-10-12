
package com.vynk.auth.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String senderEmail;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String senderEmail) {

        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
    }

    public void sendVerificationEmail(
            String recipientEmail,
            String verificationLink) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(recipientEmail);
        message.setSubject("Verify your VYNK account");

        message.setText(
                "Welcome to VYNK!\n\n"
                + "You're one step away from connecting with purpose.\n\n"
                + "Click the link below to verify your email address:\n\n"
                + verificationLink
                + "\n\nThis link expires in 24 hours.\n\n"
                + "If you did not create a VYNK account, "
                + "you can safely ignore this email."
        );

        mailSender.send(message);
    }

    public void sendPasswordResetEmail(
            String recipientEmail,
            String resetLink) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(recipientEmail);
        message.setSubject("Reset your VYNK password");

        message.setText(
                "Hello from VYNK,\n\n"
                + "We received a request to reset the password for your VYNK account.\n\n"
                + "Click the link below to set a new password:\n\n"
                + resetLink
                + "\n\nThis link expires in 1 hour and can only be used once.\n\n"
                + "If you did not request a password reset, you can safely ignore this email. "
                + "Your account remains secure."
        );

        mailSender.send(message);
    }
}
