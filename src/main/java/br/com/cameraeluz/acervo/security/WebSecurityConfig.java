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
 * and role-based access control for all API endpoints.</p>
 *
 * <h2>Photo visibility</h2>
 * <p>Access to photo content is governed by a per-photo
 * {@link br.com.cameraeluz.acervo.models.enums.Visibility} tier rather than a
 * global runtime toggle. The filter chain grants the minimum necessary access at
 * the transport layer; fine-grained enforcement is handled at the application layer:</p>
 * <ul>
 *   <li>{@code GET /api/photos/view/**} is {@code permitAll} so that
 *       {@link br.com.cameraeluz.acervo.models.enums.Visibility#OPEN} photos are
 *       reachable without authentication. The controller enforces the photo's
 *       visibility tier and returns {@code 404} for inaccessible resources to avoid
 *       leaking their existence.</li>
 *   <li>{@code GET /api/photos}, {@code GET /api/photos/search}, and
 *       {@code GET /api/photos/{id}} require authentication. Results are further
 *       scoped by the caller's visibility permissions (query-layer filtering via
 *       {@link br.com.cameraeluz.acervo.repositories.specs.PhotoSpecifications} and
 *       controller-layer checks via
 *       {@link br.com.cameraeluz.acervo.services.PhotoService#isVisibleTo}).</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // View endpoint: open at the filter-chain level so OPEN photos are
                        // accessible without a JWT. Per-photo visibility is enforced in
                        // PhotoController.viewPhoto() — inaccessible photos return 404.
                        .requestMatchers("/api/photos/view/**").permitAll()

                        // Listing and single-photo read endpoints: require authentication.
                        // Visibility scoping is applied at the query / application layer.
                        .requestMatchers(HttpMethod.GET, "/api/photos").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/photos/search").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/photos/{id}").authenticated()

                        // Download: any authenticated user — fine-grained permission check
                        // is in DownloadPermissionService. Visibility is orthogonal to downloads.
                        .requestMatchers(HttpMethod.GET, "/api/photos/download/**").authenticated()

                        // Download permission management (specific before generic)
                        .requestMatchers(HttpMethod.GET, "/api/downloads/permissions").hasAnyRole("ADMIN", "EDITOR")
                        .requestMatchers(HttpMethod.POST, "/api/downloads/permissions").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/downloads/permissions/**").authenticated()

                        // Tracks require any level of authentication
                        .requestMatchers("/api/tracks/**").hasAnyRole("ADMIN", "EDITOR", "AUTHOR", "GUEST")

                        // Upload and Edit/Delete
                        .requestMatchers(HttpMethod.POST, "/api/photos/upload").hasAnyRole("ADMIN", "EDITOR", "AUTHOR")
                        .requestMatchers(HttpMethod.PUT, "/api/photos/**").hasAnyRole("ADMIN", "EDITOR", "AUTHOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/photos/**").hasAnyRole("ADMIN", "EDITOR", "AUTHOR")

                        .anyRequest().authenticated()
                );

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
