package com.team2.postservice.config; // 패키지 경로는 프로젝트에 맞게 조절해주세요

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 프로젝트 루트의 uploads 디렉토리 절대 경로 추출
        Path uploadPath = Paths.get("uploads").toAbsolutePath().normalize();
        String resourceLocation = uploadPath.toUri().toString();

        // /images/** 패턴으로 요청이 오면 uploads 폴더에서 파일을 찾아서 반환
        registry.addResourceHandler("/images/**")
                .addResourceLocations(resourceLocation);
    }
}