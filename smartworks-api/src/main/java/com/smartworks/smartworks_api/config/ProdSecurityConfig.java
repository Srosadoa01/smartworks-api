package com.smartworks.smartworks_api.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.smartworks.smartworks_api.security.JwtAuthFilter;

@Configuration
@Profile("prod")
public class ProdSecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public ProdSecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Rutas públicas
                        .requestMatchers("/health", "/error").permitAll()
                        .requestMatchers("/auth/**").permitAll()

                        // Dashboard: USER y ADMIN
                        .requestMatchers("/dashboard/**").hasAnyRole("USER", "ADMIN")

                        // Productos
                        .requestMatchers(HttpMethod.GET, "/products/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/products/**").hasRole("ADMIN")

                        // Clientes
                        .requestMatchers(HttpMethod.GET, "/customers/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/customers/**").hasAnyRole("USER", "ADMIN")

                        // USER y ADMIN pueden subir foto al crear cliente
                        .requestMatchers(HttpMethod.PATCH, "/customers/*/photo").hasAnyRole("USER", "ADMIN")

                        // Solo ADMIN puede editar/eliminar clientes existentes
                        .requestMatchers(HttpMethod.PUT, "/customers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/customers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/customers/**").hasRole("ADMIN")

                        // Pedidos
                        .requestMatchers(HttpMethod.GET, "/orders/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/orders/**").hasAnyRole("USER", "ADMIN")

                        // Solo ADMIN cambia estado o modifica pedidos
                        .requestMatchers(HttpMethod.PUT, "/orders/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/orders/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/orders/**").hasRole("ADMIN")

                        // Chatbot: USER y ADMIN
                        .requestMatchers("/chat/**").hasAnyRole("USER", "ADMIN")

                        // Analizador y reportes: solo ADMIN
                        .requestMatchers("/analytics/**").hasRole("ADMIN")
                        .requestMatchers("/reports/**").hasRole("ADMIN")

                        // Cualquier otra ruta necesita login
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}