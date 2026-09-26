package com.sehaaz.eventtix.notification.domain;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailService {

    private static final String FROM = "noreply@eventtix.local";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void send(String to, String subject, String template, Map<String, Object> variables) {
        String html = templateEngine.process(template, new Context(null, variables));
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(FROM);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
        } catch (MessagingException e) {
            throw new IllegalStateException("Mail oluşturulamadı: " + to, e);
        }
        mailSender.send(message);
    }
}
