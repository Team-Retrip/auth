package com.retrip.auth.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.password-reset.url:http://localhost:3000/reset-password}")
    private String resetPasswordUrl;

    public void sendPasswordResetEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("[Retrip] 비밀번호 재설정 안내");
        message.setText(buildEmailBody(token));
        mailSender.send(message);
    }

    private String buildEmailBody(String token) {
        return String.format("""
                안녕하세요, Retrip입니다.

                아래 링크를 클릭하여 비밀번호를 재설정해 주세요.
                링크는 30분 동안 유효하며, 1회만 사용할 수 있습니다.

                %s?token=%s

                본인이 요청하지 않은 경우 이 메일을 무시하셔도 됩니다.

                감사합니다.
                Retrip 팀
                """, resetPasswordUrl, token);
    }
}
