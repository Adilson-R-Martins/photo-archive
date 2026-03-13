package br.com.cameraeluz.acervo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

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
        http.csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers("/error").permitAll()
                                // Visualização deve ser permitida para todos (público)
                                .requestMatchers("/api/photos/view/**").permitAll()
                                // Download e Tracks exigem qualquer nível de autenticação
                                .requestMatchers("/api/photos/download/**").hasAnyRole("ADMIN", "EDITOR", "AUTHOR", "GUEST")
                                .requestMatchers("/api/tracks/**").hasAnyRole("ADMIN", "EDITOR", "AUTHOR", "GUEST")
                                // Upload e Edição/Delete (Soft Delete)
                                .requestMatchers(HttpMethod.POST, "/api/photos/upload").hasAnyRole("ADMIN", "EDITOR", "AUTHOR")
                                .requestMatchers(HttpMethod.PUT, "/api/photos/**").hasAnyRole("ADMIN", "EDITOR", "AUTHOR")
                                .requestMatchers(HttpMethod.DELETE, "/api/photos/**").hasAnyRole("ADMIN", "EDITOR", "AUTHOR")
                                .anyRequest().authenticated()
                );

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}