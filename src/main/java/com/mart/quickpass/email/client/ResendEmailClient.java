package com.mart.quickpass.email.client;

import com.mart.quickpass.email.exception.EmailSendFailedException;
import com.mart.quickpass.global.config.ResendProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResendEmailClient {
    // Redsnd API와 통신하는 클라이언트 계층

    private static final String RESEND_API_URL = "https://api.resend.com";

    private final ResendProperties resendProperties;

    public void sendVerificationCode(String email, String code) {
        sendEmail(
                email,
                "QuickPass 이메일 인증번호",
                "<p>QuickPass 이메일 인증번호는 <strong>" + code + "</strong> 입니다.</p>"
        );
    }

    public void sendPasswordResetCode(String email, String code) {
        sendEmail(
                email,
                "QuickPass 비밀번호 재설정 인증번호",
                "<p>QuickPass 비밀번호 재설정 인증번호는 <strong>" + code + "</strong> 입니다.</p>"
        );
    }

    public void sendPasswordChangedNotice(String email) {
        sendEmail(
                email,
                "QuickPass 비밀번호 변경 안내",
                "<p>QuickPass 계정의 비밀번호가 변경되었습니다.</p>"
                        + "<p>본인이 변경하지 않았다면 고객 지원팀에 문의해 주세요.</p>"
        );
    }

    private void sendEmail(String email, String subject, String html) {
        if (!StringUtils.hasText(resendProperties.apiKey()) || !StringUtils.hasText(resendProperties.from())) {
            log.error("Resend 설정이 누락되었습니다. RESEND_API_KEY와 RESEND_FROM_EMAIL을 설정하세요.");
            throw new EmailSendFailedException();
        }

        try {
            RestClient.builder()
                    .baseUrl(RESEND_API_URL)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + resendProperties.apiKey())
                    .build()
                    .post()
                    .uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ResendEmailRequest(
                            resendProperties.from(),
                            email,
                            subject,
                            html
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Resend 이메일 전송 실패: email={}", email, e);
            throw new EmailSendFailedException();
        }
    }

    private record ResendEmailRequest(String from, String to, String subject, String html) {
    }
}
