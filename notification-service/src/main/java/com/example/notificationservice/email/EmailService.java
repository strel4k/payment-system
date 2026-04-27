package com.example.notificationservice.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    public void sendEmail(String to, String subject, String body) {
        log.info("Sending email: to={} subject={}", to, subject);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailProperties.getFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);

            log.debug("Email sent successfully: to={} subject={}", to, subject);
        } catch (MailException e) {
            log.error("Failed to send email: to={} subject={}", to, subject, e);
            throw e;
        }
    }
}