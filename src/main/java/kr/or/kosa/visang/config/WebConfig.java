package kr.or.kosa.visang.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.upload-dir.pdf}")
    private String uploadDirPdf;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /images/profile/** → upload/profile 폴더 매핑
        registry.addResourceHandler("/images/profile/**")
                .addResourceLocations("file:" + uploadDir + "/");

        // PDF 파일 매핑
        registry.addResourceHandler("/files/pdf/**")
                .addResourceLocations("file:" + uploadDirPdf + "/");

        // 채팅 내역 다운로드 파일
        // /files/** 요청 -> data/chats/ 폴더 매핑
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:./data/chats/");
    }
}