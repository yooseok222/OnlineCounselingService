package kr.or.kosa.visang.config;

import kr.or.kosa.visang.common.interceptor.ConsultRoomInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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