package kr.or.kosa.visang.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("visang.service@gmail.com");  // 보내는 사람 -> 바꿔도 그대로
        message.setTo(to);                         // 받는 사람
        message.setSubject(subject);               // 제목
        message.setText(text);                     // 본문

        mailSender.send(message);
    }
}
