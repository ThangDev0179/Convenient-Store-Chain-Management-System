package com.retail.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfigurerImpl implements WebMvcConfigurer {

    @Autowired
    private ForceChangePasswordInterceptor forceChangePasswordInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(forceChangePasswordInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/forgot-password", "/css/**", "/js/**");
    }
}