package com.healthvault.platform.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Wires the JWT filter onto the protected routes and registers the {@code @CurrentUser} resolver.
 * Public routes ({@code /auth/register}, {@code /auth/login}, {@code /auth/oauth/*}, {@code /auth/guest},
 * actuator health) are simply not listed here, so the filter never runs on them.
 */
@Configuration
public class WebSecurityConfig implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilter(JwtService jwt) {
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>(new JwtAuthFilter(jwt));
        reg.addUrlPatterns("/documents", "/documents/*", "/auth/me");
        reg.setName("jwtAuthFilter");
        return reg;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }
}
