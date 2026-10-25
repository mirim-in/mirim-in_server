package com.mirim.mirimin.global.mail;

import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String from;

    public MailService(JavaMailSender mailSender, @Value("${spring.mail.username:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    /**
     * MAIL_USERNAME이 비어 있으면(로컬 개발) 보내지 않고 내용을 로그로 남긴다.
     * 배포 환경에서는 반드시 메일 설정을 채워야 한다.
     */
    public void send(String to, String subject, String text) {
        if (from.isBlank()) {
            log.warn("[메일 미설정 - 발송 생략] to={}, subject={}\n{}", to, subject, text);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("메일 발송 실패 to={}", to, e);
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED);
        }
    }
}
