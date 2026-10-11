package com.scribl.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CurrentPlayerArgumentResolver currentPlayerResolver;

    public WebConfig(CurrentPlayerArgumentResolver currentPlayerResolver) {
        this.currentPlayerResolver = currentPlayerResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentPlayerResolver);
    }
}