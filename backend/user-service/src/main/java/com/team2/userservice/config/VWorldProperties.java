package com.team2.userservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vworld")
@Getter
@Setter
public class VWorldProperties {
    @Value("${vworld.api-key}")
    private String apiKey;
}