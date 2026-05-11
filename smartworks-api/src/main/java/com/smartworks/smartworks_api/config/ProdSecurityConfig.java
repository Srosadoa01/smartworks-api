package com.smartworks.smartworks_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("prod")
public class ProdSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // TEMPORAL para probar la API desplegada
                        .requestMatchers("/products/**").permitAll()
                        .requestMatchers("/customers/**").permitAll()
                        .requestMatchers("/orders/**").permitAll()
                        .requestMatchers("/chat/**").permitAll()
                        .requestMatchers("/analytics/**").permitAll()
                        .requestMatchers("/reports/**").permitAll()
                        .anyRequest().authenticated());

        // Luego aquí metemos el JwtAuthFilter cuando lo reactivemos
        // .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
