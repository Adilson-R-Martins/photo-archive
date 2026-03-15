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
import java.util.Arrays;

/**
 * Spring Security configuration for the Photo Archive application.
 *
 * <p>Configures stateless JWT-based authentication, CORS, method-level security,
 * and role-based access control for all API endpoints. Photo visibility is
 * runtime-configurable via the {@code photoarchive.app.photo-visibility} property.</p>
 */
@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    @Value("${photoarchive.app.photo-visibility:PRIVATE}")
    private String photoVisibility;

    /**
     * Comma-separated list of allowed CORS origins.
     * Override via the {@code ALLOWED_ORIGINS} environment variable in each deployment environment.
     * Never use a wildcard ({@code *}) together with {@code allowCredentials(true)}.
     */
    @Value("${photoarchive.app.allowed-origins}")
    private String allowedOrigins;

    private final AuthEntryPointJwt unauthorizedHandler;

    public WebSecurityConfig(AuthEntryPointJwt unauthorizedHandler) {
        this.unauthorizedHandler = unauthorizedHandler;
    }

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the security filter chain: CSRF disabled (stateless API),
     * JWT filter injected before the username/password filter, and endpoint-level
     * authorization rules.
     *
     * <p>Photo visibility is runtime-configurable; PUBLIC mode opens the view
     * and search endpoints to anonymous users.</p>
     *
     * @param http the {@link HttpSecurity} builder provided by Spring.
     * @return the built {@link SecurityFilterChain}.
     * @throws Exception if the configuration cannot be applied.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                        auth.requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers("/error").permitAll();
                        // Photo visibility is runtime-configurable; PUBLIC mode opens view and search to anonymous users.
                        if ("PUBLIC".equalsIgnoreCase(photoVisibility)) {
                            auth.requestMatchers("/api/photos/view/**").permitAll()
                                    .requestMatchers(HttpMethod.GET, "/api/photos").permitAll()
                                    .requestMatchers(HttpMethod.GET, "/api/photos/search").permitAll()
                                    .requestMatchers(HttpMethod.GET, "/api/photos/{id}").permitAll();
                        } else {
                            auth.requestMatchers("/api/photos/view/**").authenticated()
                                    .requestMatchers(HttpMethod.GET, "/api/photos").authenticated()
                                    .requestMatchers(HttpMethod.GET, "/api/photos/search").authenticated()
                                    .requestMatchers(HttpMethod.GET, "/api/photos/{id}").authenticated();
                        }
                        // Download: any authenticated user — fine-grained permission check is in DownloadPermissionService
                        auth.requestMatchers(HttpMethod.GET, "/api/photos/download/**").authenticated()
                                // Download permission management (order: specific before generic)
                                .requestMatchers(HttpMethod.GET, "/api/downloads/permissions").hasAnyRole("ADMIN", "EDITOR")
                                .requestMatchers(HttpMethod.POST, "/api/downloads/permissions").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/api/downloads/permissions/**").authenticated()
                                // Tracks require any level of authentication
                                .requestMatchers("/api/tracks/**").hasAnyRole("ADMIN", "EDITOR", "AUTHOR", "GUEST")
                                // Upload and Edit/Delete (Soft Delete)
                                .requestMatchers(HttpMethod.POST, "/api/photos/upload").hasAnyRole("ADMIN", "EDITOR", "AUTHOR")
                                .requestMatchers(HttpMethod.PUT, "/api/photos/**").hasAnyRole("ADMIN", "EDITOR", "AUTHOR")
                                .requestMatchers(HttpMethod.DELETE, "/api/photos/**").hasAnyRole("ADMIN", "EDITOR", "AUTHOR")
                                .anyRequest().authenticated();
                });

        // authTokenFilter runs before the standard username/password filter (JWT chain).
        // rateLimitFilter runs before authTokenFilter so abusive IPs are rejected first,
        // before any DB or JWT processing occurs.
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter(), AuthTokenFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Origins are loaded from photoarchive.app.allowed-origins (env: ALLOWED_ORIGINS).
        // Supports multiple origins separated by commas, e.g.: https://app.domain.com,https://admin.domain.com
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
