package com.example.notificationservice.email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailProperties emailProperties;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("sendEmail — отправляет письмо с корректными полями")
    void sendEmail_sendsWithCorrectFields() {
        when(emailProperties.getFrom()).thenReturn("noreply@payment-system.com");

        emailService.sendEmail("user@test.com", "REGISTRATION", "Welcome to Payment System!");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo("noreply@payment-system.com");
        assertThat(sent.getTo()).containsExactly("user@test.com");
        assertThat(sent.getSubject()).isEqualTo("REGISTRATION");
        assertThat(sent.getText()).isEqualTo("Welcome to Payment System!");
    }

    @Test
    @DisplayName("sendEmail — пробрасывает MailException при ошибке отправки")
    void sendEmail_rethrowsMailException() {
        when(emailProperties.getFrom()).thenReturn("noreply@payment-system.com");
        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.sendEmail("user@test.com", "REGISTRATION", "Welcome!"))
                .isInstanceOf(MailSendException.class)
                .hasMessageContaining("SMTP unavailable");
    }
}