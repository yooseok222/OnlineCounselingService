package kr.or.kosa.visang.common.config.security.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * 이메일 인증이 완료되지 않은 사용자가 로그인을 시도할 때 throw 되는 예외.
 */
public class EmailNotVerifiedException extends AuthenticationException {

    public EmailNotVerifiedException(String msg) {
        super(msg);
    }
} 