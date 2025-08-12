package kr.or.kosa.visang.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.upload-dir.pdf}")
    private String uploadDirPdf;

    @Value("${file.upload-dir.signed-pdf}")
    private String uploadDirSignedPdf;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /images/profile/** → upload/profile 폴더 매핑
        registry.addResourceHandler("/images/profile/**")
                .addResourceLocations("file:" + uploadDir + "/");

        // PDF 파일 매핑
        registry.addResourceHandler("/files/pdf/**")
                .addResourceLocations("file:" + uploadDirPdf + "/");

        // PDF 파일 매핑
        registry.addResourceHandler("/files/signed-pdf/**")
                .addResourceLocations("file:" + uploadDirSignedPdf + "/");

        // 채팅 내역 다운로드 파일
        // /files/** 요청 -> data/chats/ 폴더 매핑
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:./data/chats/");
    }

    // 세션 기반 Locale Resolver
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        slr.setDefaultLocale(Locale.KOREAN); // 기본 언어 설정
        return slr;
    }

    // 언어 변경 감지 인터셉터
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        lci.setParamName("lang"); // ex: ?lang=en
        return lci;
    }

    // 인터셉터 등록
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}