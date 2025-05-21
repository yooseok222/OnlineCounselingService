package kr.or.kosa.visang.config;

import kr.or.kosa.visang.common.interceptor.ConsultRoomInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 설정 클래스
 * 
 * 참고: 폰트 자동 적용은 WebMvcConfigurer를 통해 구현하지 않고,
 * layout/main.html과 fragments/header.html에 font.css를 include하는 방식을 사용합니다.
 * 이 방식이 Thymeleaf 템플릿 엔진의 특성과 잘 맞습니다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private ConsultRoomInterceptor consultRoomInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(consultRoomInterceptor)
                .addPathPatterns("/consult/room");
    }
} 