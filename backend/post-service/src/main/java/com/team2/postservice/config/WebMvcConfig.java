package com.team2.postservice.config; // 패키지 경로는 프로젝트에 맞게 조절해주세요

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 프로젝트 루트의 uploads 디렉토리 절대 경로 추출
        Path uploadPath = Paths.get("uploads").toAbsolutePath().normalize();
        String resourceLocation = uploadPath.toUri().toString();

        // /images/** 패턴으로 요청이 오면 uploads 폴더에서 파일을 찾아서 반환.
        // 저장 파일명이 매번 새 UUID라 같은 URL이 다른 내용으로 바뀌는 일이 없으므로
        // 캐시 헤더 없이 서빙하던 것을 영구 캐시로 바꾼다 — 글 목록을 다시 열거나
        // 뒤로가기할 때마다 같은 사진을 매번 새로 받아오던 게 느려 보이는 원인이었다.
        registry.addResourceHandler("/images/**")
                .addResourceLocations(resourceLocation)
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());
    }
}