package br.com.cameraeluz.acervo.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    @Value("${photoarchive.app.photo-visibility:PRIVATE}")
    private String photoVisibility;

    private final AuthEntryPointJwt unauthorizedHandler;

    public WebSecurityConfig(AuthEntryPointJwt unauthorizedHandler) {
        this.unauthorizedHandler = unauthorizedHandler;
    }

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                        auth.requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers("/error").permitAll();
                        // Visibilidade configurável: PUBLIC permite acesso sem autenticação
                        if ("PUBLIC".equalsIgnoreCase(photoVisibility)) {
                            auth.requestMatchers("/api/photos/view/**").permitAll()
                                    .requestMatchers(HttpMethod.GET, "/api/photos/search").permitAll();
                        } else {
                            auth.requestMatchers("/api/photos/view/**").authenticated()
                                    .requestMatchers(HttpMethod.GET, "/api/photos/search").authenticated();
                        }
                        // Download: any authenticated user — fine-grained permission check is in DownloadPermissionService
                        auth.requestMatchers(HttpMethod.GET, "/api/photos/download/**").authenticated()
                                // Download permission management (order: specific before generic)
                                .requestMatchers(HttpMethod.GET, "/api/downloads/permissions").hasAnyRole("ADMIN", "EDITOR")
                                .requestMatchers(HttpMethod.POST, "/api/downloads/permissions").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/api/downloads/permissions/**").authenticated()
                                // Tracks require any level of authentication
                                .requestMatchers("/api/tracks/**").hasAnyRole("ADMIN", "EDITOR", "AUTHOR", "GUEST")
                                // Upload e Edição/Delete (Soft Delete)
                                .requestMatchers(HttpMethod.POST, "/api/photos/upload").hasAnyRole("ADMIN", "EDITOR", "AUTHOR")
                                .requestMatchers(HttpMethod.PUT, "/api/photos/**").hasAnyRole("ADMIN", "EDITOR", "AUTHOR")
                                .requestMatchers(HttpMethod.DELETE, "/api/photos/**").hasAnyRole("ADMIN", "EDITOR", "AUTHOR")
                                .anyRequest().authenticated();
                });

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://seudominio.com.br")); // ← sua origem real
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}