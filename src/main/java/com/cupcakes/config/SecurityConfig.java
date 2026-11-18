package com.cupcakes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
public class SecurityConfig {

    private static final String[] ROTAS_PUBLICAS = {
            "/",
            "/*.html",
            "/admin/**",
            "/css/**",
            "/js/**",
            "/images/**",
            "/actuator/health",
            "/api/auth/**",
            "/api/categorias",
            "/api/opcoes-personalizacao",
            "/api/produtos",
            "/api/produtos/**"
    };

    private static final String[] ROTAS_CLIENTE = {
            "/api/pedidos",
            "/api/pedidos/**"
    };

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        AuthenticationEntryPoint authenticationEntryPoint,
        AccessDeniedHandler accessDeniedHandler,
        JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        configurarTratamentoDeAcesso(http, authenticationEntryPoint, accessDeniedHandler);
        configurarAutorizacao(http);
        configurarResourceServer(http, jwtAuthenticationConverter);
        return http.build();
    }

    private void configurarTratamentoDeAcesso(
        HttpSecurity http,
        AuthenticationEntryPoint authenticationEntryPoint,
        AccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));
    }

    private void configurarAutorizacao(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(ROTAS_PUBLICAS).permitAll()
                .requestMatchers(ROTAS_CLIENTE).hasRole("CLIENTE")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated());
    }

    private void configurarResourceServer(
        HttpSecurity http,
        JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        http.oauth2ResourceServer(resourceServer -> resourceServer
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
    }
}